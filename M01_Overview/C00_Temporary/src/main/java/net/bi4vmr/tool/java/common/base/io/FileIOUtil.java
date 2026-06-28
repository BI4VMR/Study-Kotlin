package net.bi4vmr.tool.java.common.base.io;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * 文件输入与输出工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class FileIOUtil extends IOUtil {

    /*
     * ----- 从文件读取二进制数据 -----
     */

    /**
     * 从文件读取二进制数据。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param file   目标文件。
     * @param offset 起始位置（从 {@code 0} 开始计数）。
     * @param length 读取字节数。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(File file, long offset, int length) {
        // 校验文件是否可读
        if (file == null || !file.exists() || file.isDirectory() || !file.canRead()) {
            System.err.println("FileIOUtil - File not exist or no permission to read!");
            return new byte[0];
        }

        // 校验输入参数
        if (offset < 0 || offset >= file.length() || length < 0) {
            System.err.println("FileIOUtil - Offset or length value invalid!");
            return new byte[0];
        }

        // 如果参数指定的长度大于实际数据长度，则改写为实际数据长度。
        long maxLength = file.length() - offset;
        if (length > maxLength) {
            // 输入参数为"int"类型，已确认数值大于"long"类型值，因此"long"类型值必然在"int"范围内，可以安全地窄化转换。
            length = (int) maxLength;
        }

        byte[] buffer = new byte[length];
        try (
                RandomAccessFile accessor = new RandomAccessFile(file, "r");
        ) {
            // 忽略指定长度的数据
            accessor.seek(offset);

            int count = accessor.read(buffer);
            // 如果实际读取的数据长度小于目标长度，则截取有效元素。
            if (count < length) {
                buffer = Arrays.copyOf(buffer, count);
            }

            return buffer;
        } catch (IOException e) {
            System.err.println("FileIOUtil: Read file as bytes failed! Reason:[" + e.getMessage() + "]");
        }

        return new byte[0];
    }

    /**
     * 从文件读取二进制数据。
     * <p>
     * 该方法将从文件起始位置开始，读取第二参数"length"指定长度的数据。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param file   目标文件。
     * @param length 读取字节数。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(File file, int length) {
        return readAsBytes(file, 0L, length);
    }

    /**
     * 从文件读取二进制数据。
     * <p>
     * 该方法将从文件起始位置开始，读取最多 {@link Integer#MAX_VALUE} 字节的数据，文件体积较大时需要注意内存占用问题。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param file 目标文件。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(File file) {
        return readAsBytes(file, 0L, Integer.MAX_VALUE);
    }


    /*
     * ----- 从文件描述符读取二进制数据 -----
     */

    /**
     * 从文件描述符读取二进制数据。
     * <p>
     * 该方法将从第二参数"offset"指定位置开始，读取第三参数"length"指定长度的数据。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param fd         文件描述符。
     * @param offset     起始位置（从0开始计数）。
     * @param length     读取字节数。
     * @param bufferSize 缓冲区大小（字节）。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(FileDescriptor fd, long offset, int length, int bufferSize) {
        // 校验文件描述符是否可用
        if (fd == null || !fd.valid()) {
            System.err.println("FileIOUtil - FileDescriptor is null or invalid!");
            return new byte[0];
        }

        // 校验输入参数
        if (offset < 0 || length < 0 || bufferSize <= 0) {
            System.err.println("FileIOUtil - Offset or length value invalid!");
            return new byte[0];
        }

        ByteArrayOutputStream result = null;
        try (
                FileInputStream fis = new FileInputStream(fd);
                BufferedInputStream bis = new BufferedInputStream(fis, bufferSize)
        ) {
            // 忽略指定长度的数据
            if (offset > 0L) {
                long skipped = 0;
                while (skipped < offset) {
                    long count = bis.skip(offset - skipped);
                    if (count <= 0) {
                        // 跳过操作失败，检测是否已到达末尾。
                        if (bis.read() == -1) {
                            return new byte[0];
                        }

                        skipped++;
                    } else {
                        // 跳过操作成功，累计偏移量。
                        skipped += count;
                    }
                }
            }

            result = new ByteArrayOutputStream();
            byte[] buffer = new byte[bufferSize];
            int remaining = length;
            while (remaining > 0) {
                // 当前轮次读取的数量为缓冲区容量和剩余数量中较小的一个
                int readCount = Math.min(bufferSize, remaining);
                int count = bis.read(buffer, 0, readCount);
                // 如果读取方法返回负数，表示已到文件末尾。
                if (count == -1) {
                    break;
                }

                if (count > 0) {
                    // 将读取到的数据添加到结果列表中
                    result.write(buffer, 0, count);
                    // 更新剩余的数据量
                    remaining -= count;
                }
            }

            // 将每轮读取到的数据合并为单个数组
            return result.toByteArray();
        } catch (IOException e) {
            System.err.println("FileIOUtil - Read FD as bytes failed! Reason:[" + e.getMessage() + "]");
        } finally {
            closeSilently(result);
        }

        return new byte[0];
    }

    /**
     * 从文件描述符读取二进制数据。
     * <p>
     * 该方法将从第二参数"offset"指定位置开始，读取第三参数"length"指定长度的数据。
     * <p>
     * 遇到异常时默认返回内容为空的数组；默认使用"8KB"缓冲区。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param fd     文件描述符。
     * @param offset 起始位置（从0开始计数）。
     * @param length 读取字节数。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(FileDescriptor fd, long offset, int length) {
        return readAsBytes(fd, offset, length, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 从文件描述符读取二进制数据。
     * <p>
     * 该方法将从文件起始位置开始，读取第二参数"length"指定长度的数据。
     * <p>
     * 遇到异常时默认返回内容为空的数组；默认使用"8KB"缓冲区。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param fd     文件描述符。
     * @param length 读取字节数。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(FileDescriptor fd, int length) {
        return readAsBytes(fd, 0L, length, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 从文件描述符读取二进制数据。
     * <p>
     * 该方法将从文件起始位置开始，读取最多 {@link Integer#MAX_VALUE} 字节的数据，文件体积较大时需要注意内存占用问题。
     * <p>
     * 遇到异常时默认返回内容为空的数组；默认使用"8KB"缓冲区。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param fd 文件描述符。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的数组。
     */
    public static byte[] readAllAsBytes(FileDescriptor fd) {
        return readAsBytes(fd, 0L, Integer.MAX_VALUE, BUFFER_SIZE_DEFAULT);
    }


    /*
     * ----- 从文件读取二进制数据，并进行处理。 -----
     */

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 该方法将从文件起始位置开始，读取最多{@link Integer#MAX_VALUE}字节的数据，文件体积较大时需要注意内存占用问题。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param file 目标文件。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的字符串。
     */
    public static String readAsHexText(File file) {
        return readAsHexText(file, 0, Integer.MAX_VALUE);
    }

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 该方法将从文件起始位置开始，读取第二参数"length"指定长度的数据，并转换为十六进制文本。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param file   目标文件。
     * @param length 读取字节数。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的字符串。
     */
    public static String readAsHexText(File file, int length) {
        return readAsHexText(file, 0, length);
    }

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 该方法将从第二参数"offset"指定位置开始，读取第三参数"length"指定长度的数据，并转换为十六进制文本。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param file   目标文件。
     * @param offset 起始位置（从0开始计数）。
     * @param length 读取字节数。
     * @return 二进制数据。永不为空值，读取失败时将返回内容为空的字符串。
     */
    public static String readAsHexText(File file, int offset, int length) {
        byte[] datas = readAsBytes(file, offset, length);
        return toHexString(datas, true, true);
    }


    /*
     * ----- 将输入流的数据转存至文件 -----
     */

    /**
     * 将输入流中的数据转存至文件。
     * <p>
     * 操作完毕后输入流会被关闭。
     *
     * @param stream     输入流。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     */
    public static void saveToFile(InputStream stream, File file, int bufferSize) {
        // 校验输入参数
        if (bufferSize <= 0) {
            System.err.println("FileIOUtil - Buffer size must > 0!");
            return;
        }

        try (
                BufferedInputStream bis = new BufferedInputStream(stream, bufferSize);
                BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(file.toPath()), bufferSize)
        ) {
            byte[] buffer = new byte[bufferSize];
            while (true) {
                int count = bis.read(buffer);
                if (count == -1) {
                    break;
                }
                bos.write(buffer, 0, count);
            }
        } catch (IOException e) {
            System.err.println("FileIOUtil - Copy data from InputStream failed! Reason:[" + e.getMessage() + "]");
        }
    }

    /**
     * 将输入流中的数据转存至文件。
     * <p>
     * 操作完毕后输入流会被关闭；缓冲区容量默认为8KB。
     *
     * @param stream 输入流。
     * @param dest   目标文件。
     */
    public static void saveToFile(InputStream stream, File dest) {
        saveToFile(stream, dest, BUFFER_SIZE_DEFAULT);
    }


    /*
     * ----- 将数组流的数据转存至文件 -----
     */

    /**
     * 将 {@link ByteArrayOutputStream} 中的数据转存至文件。
     *
     * @param stream     {@link ByteArrayOutputStream}实例。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     */
    public static void saveToFile(ByteArrayOutputStream stream, File file, int bufferSize) {
        ByteArrayInputStream input = new ByteArrayInputStream(stream.toByteArray());
        saveToFile(input, file, bufferSize);
    }

    /**
     * 将 {@link ByteArrayOutputStream} 中的数据转存至文件。
     * <p>
     * 默认使用"8KB"缓冲区。
     *
     * @param stream {@link ByteArrayOutputStream}实例。
     * @param file   目标文件。
     */
    public static void saveToFile(ByteArrayOutputStream stream, File file) {
        saveToFile(stream, file, BUFFER_SIZE_DEFAULT);
    }


    /*
     * ----- 将字节数组转存至文件 -----
     */

    /**
     * 将字节数组中的数据转存至文件。
     *
     * @param data       字节数组。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     */
    public static void saveToFile(byte[] data, File file, int bufferSize) {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        saveToFile(input, file, bufferSize);
    }

    /**
     * 将字节数组中的数据转存至文件。
     * <p>
     * 默认使用"8KB"缓冲区。
     *
     * @param data 字节数组。
     * @param file 目标文件。
     */
    public static void saveToFile(byte[] data, File file) {
        saveToFile(data, file, BUFFER_SIZE_DEFAULT);
    }


    /*
     * ----- 将文件描述符指向的内容转存至文件 -----
     */

    /**
     * 将文件描述符指向的内容转存至文件。
     *
     * @param fd         文件描述符。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     */
    public static void saveToFile(FileDescriptor fd, File file, int bufferSize) {
        // 校验输入参数的合法性
        if (bufferSize <= 0) {
            System.err.println("FileIOUtil - Buffer size must > 0!");
            return;
        }

        try (
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fd), bufferSize);
                BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(file.toPath()), bufferSize)
        ) {
            byte[] buffer = new byte[bufferSize];
            while (true) {
                int count = bis.read(buffer);
                if (count == -1) {
                    break;
                }
                bos.write(buffer, 0, count);
            }
        } catch (Exception e) {
            System.err.println("FileIOUtil - Copy data from FD failed! Reason:[" + e.getMessage() + "]");
        }
    }

    /**
     * 将文件描述符指向的内容转存至文件。
     * <p>
     * 默认使用"8KB"缓冲区。
     *
     * @param fd   文件描述符。
     * @param file 目标文件。
     */
    public static void saveToFile(FileDescriptor fd, File file) {
        saveToFile(fd, file, BUFFER_SIZE_DEFAULT);
    }


    /**
     * 将数字转为十六进制文本。
     * <p>
     * 与 [lib-common:base#NumberUtil] 模块中的同名方法相同。
     *
     * @param data        待转换的数字。
     * @param needPadding 结果为单个字符时，是否在前面补"0"。
     * @param isUpperCase 是否将结果转换为大写字母。
     * @return 十六进制文本。永不为空，转换失败时将返回内容为空的字符串。
     */
    private static String toHexString(byte data, boolean needPadding, boolean isUpperCase) {
        /*
         * 单个字节的取值范围是：[0, 255]，当它被读取为"byte"类型时，大于127的值将被映射到负数，因此我们需要将"byte"值和"0xFF"做与运
         * 算，丢弃符号位以获取原始数值。
         */
        String hex = Integer.toHexString(data & 0xFF);
        // 如果该字节对应的16进制文本只有一个字符，则在前面补"0"。
        if (needPadding && hex.length() < 2) {
            hex = "0" + hex;
        }

        if (isUpperCase) {
            return hex.toUpperCase();
        } else {
            return hex;
        }
    }

    /**
     * 将数组转为十六进制文本。
     * <p>
     * 与 [lib-common:base#NumberUtil] 模块中的同名方法相同。
     *
     * @param datas       待转换的数组。
     * @param needPadding 元素为单个字符时，是否在前面补"0"。
     * @param isUpperCase 是否将结果转换为大写字母。
     * @return 十六进制文本。永不为空，转换失败时将返回内容为空的字符串。
     */
    public static String toHexString(byte[] datas, boolean needPadding, boolean isUpperCase) {
        if (datas == null || datas.length == 0) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();
        for (byte b : datas) {
            String hex = toHexString(b, needPadding, isUpperCase);
            buffer.append(hex);
        }

        return buffer.toString();
    }
}
