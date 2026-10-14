package net.bi4vmr.gradle.util.io;

import net.bi4vmr.gradle.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(FileIOUtil.class);


    /*
     * ----- 从文件读取二进制数据 -----
     */

    /**
     * 将文件读取为二进制数据。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param file   目标文件。
     * @param offset 起始位置（从 {@code 0} 开始计数）。
     * @param length 读取字节数。
     * @return 二进制数据。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static byte[] readAsBytesUnsafe(File file, long offset, int length) throws IOException {
        /* 校验输入参数 */
        // 校验文件是否可读
        if (file == null || !file.exists() || file.isDirectory() || !file.canRead()) {
            throw new IllegalArgumentException("File not exist or no permission to read!");
        }

        // 校验输入参数
        if (offset < 0 || offset >= file.length() || length < 0) {
            throw new IllegalArgumentException("Offset or length value invalid!");
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
        }
    }

    /**
     * 将文件读取为二进制数据。
     * <p>
     * 简化方法，从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据；其他行为详见 {@link #readAsBytesUnsafe(File, long, int)} 。
     *
     * @param file 目标文件。
     * @return 二进制数据。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static byte[] readAsBytesUnsafe(File file) throws IOException {
        return readAsBytesUnsafe(file, 0L, Integer.MAX_VALUE);
    }

    /**
     * 将文件读取为二进制数据。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param file   目标文件。
     * @param offset 起始位置（从 {@code 0} 开始计数）。
     * @param length 读取字节数。
     * @return 二进制数据。读取失败时将返回空值。
     */
    public static byte[] readAsBytes(File file, long offset, int length) {
        try {
            return readAsBytesUnsafe(file, offset, length);
        } catch (Exception e) {
            logger.error("Read file as bytes failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将文件读取为二进制数据。
     * <p>
     * 简化方法，从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据，使用 {@code 8KB} 缓冲区；其他行为详见
     * {@link #readAsBytes(File, long, int)} 。
     *
     * @param file 目标文件。
     * @return 二进制数据。读取失败时将返回空值。
     */
    public static byte[] readAsBytes(File file) {
        return readAsBytes(file, 0L, Integer.MAX_VALUE);
    }


    /*
     * ----- 从文件描述符读取二进制数据 -----
     */

    /**
     * 将文件描述符读取为二进制数据。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param fd         文件描述符。
     * @param offset     起始位置（从 {@code 0} 开始计数）。
     * @param length     读取字节数。
     * @param bufferSize 缓冲区大小（字节）。
     * @return 二进制数据。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static byte[] readAsBytesUnsafe(FileDescriptor fd, long offset, int length, int bufferSize)
            throws IOException {
        /* 校验输入参数 */
        // 校验文件描述符是否可用
        if (fd == null || !fd.valid()) {
            throw new IllegalArgumentException("FileDescriptor is null or invalid!");
        }

        // 校验输入参数
        if (offset < 0 || length < 0 || bufferSize <= 0) {
            throw new IllegalArgumentException("Offset or length value invalid!");
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
        } finally {
            closeSilently(result);
        }
    }

    /**
     * 将文件描述符读取为二进制数据。
     * <p>
     * 简化方法，从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据，使用 {@code 8KB} 缓冲区；其他行为详见
     * {@link #readAsBytesUnsafe(FileDescriptor, long, int, int)} 。
     *
     * @param fd 文件描述符。
     * @return 二进制数据。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static byte[] readAsBytesUnsafe(FileDescriptor fd) throws IOException {
        return readAsBytesUnsafe(fd, 0L, Integer.MAX_VALUE, IOUtil.BUFFER_SIZE_DEFAULT);
    }

    /**
     * 从文件描述符读取二进制数据。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param fd     文件描述符。
     * @param offset 起始位置（从 {@code 0} 开始计数）。
     * @param length 读取字节数。
     * @return 二进制数据。读取失败时将返回空值。
     */
    public static byte[] readAsBytes(FileDescriptor fd, long offset, int length, int bufferSize) {
        try {
            return readAsBytesUnsafe(fd, offset, length, bufferSize);
        } catch (Exception e) {
            logger.error("Read FileDescriptor as bytes failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 从文件描述符读取二进制数据。
     * <p>
     * 简化方法，从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据，使用 {@code 8KB} 缓冲区；其他行为详见
     * {@link #readAsBytes(FileDescriptor, long, int, int)} 。
     *
     * @param fd 文件描述符。
     * @return 二进制数据。读取失败时将返回空值。
     */
    public static byte[] readAsBytes(FileDescriptor fd) {
        return readAsBytes(fd, 0L, Integer.MAX_VALUE, BUFFER_SIZE_DEFAULT);
    }


    /*
     * ----- 从文件读取二进制数据，并进行处理。 -----
     */

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据，并转换为十六进制文本。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param file   目标文件。
     * @param offset 起始位置（从 {@code 0} 开始计数）。
     * @param length 读取字节数。
     * @return 十六进制文本。永不为空值。
     */
    public static String readAsHexTextUnsafe(File file, long offset, int length) throws IOException {
        byte[] datas = readAsBytesUnsafe(file, offset, length);
        return NumberUtil.toHexString(datas, true, true);
    }

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 简化方法，从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据。其他行为详见 {@link #readAsHexTextUnsafe(File, long, int)} 。
     *
     * @param file 目标文件。
     * @return 十六进制文本。永不为空值。
     */
    public static String readAsHexTextUnsafe(File file) throws IOException {
        return readAsHexTextUnsafe(file, 0L, Integer.MAX_VALUE);
    }

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 本方法将从第二参数 {@code offset} 指定的位置开始，读取第三参数 {@code length} 指定长度的数据，并转换为十六进制文本。
     * <p>
     * 该方法仅适用于简短的数据读取场景，无法处理长度超过 {@code 2GiB} 的部分，这是因为数组容量受到 {@link Integer#MAX_VALUE} 的限制，
     * 且读取过多数据可能导致内存溢出。对于数据量较大的场景，调用者可以分段读取并进行处理。
     *
     * @param file   目标文件。
     * @param offset 起始位置（从 {@code 0} 开始计数）。
     * @param length 读取字节数。
     * @return 十六进制文本。读取失败时将返回空值。
     */
    public static String readAsHexText(File file, long offset, int length) {
        try {
            return readAsHexTextUnsafe(file, offset, length);
        } catch (Exception e) {
            logger.error("Read file as hex text failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 从文件读取十六进制文本。
     * <p>
     * 简化方法，从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据。其他行为详见 {@link #readAsHexText(File, long, int)} 。
     *
     * @param file 目标文件。
     * @return 十六进制文本。读取失败时将返回空值。
     */
    public static String readAsHexText(File file) {
        return readAsHexText(file, 0L, Integer.MAX_VALUE);
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
    public static void saveToFileUnsafe(InputStream stream, File file, int bufferSize) throws IOException {
        // 校验输入参数
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must > 0!");
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
        }
    }

    /**
     * 将输入流中的数据转存至文件。
     * <p>
     * 简化方法，使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFileUnsafe(InputStream, File, int)} 。
     *
     * @param stream 输入流。
     * @param file   目标文件。
     */
    public static void saveToFileUnsafe(InputStream stream, File file) throws IOException {
        saveToFileUnsafe(stream, file, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 将输入流中的数据转存至文件。
     * <p>
     * 操作完毕后输入流会被关闭。
     *
     * @param stream     输入流。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     * @return {@code true} 表示操作成功；{@code false} 表示操作失败。
     */
    public static boolean saveToFile(InputStream stream, File file, int bufferSize) {
        try {
            saveToFileUnsafe(stream, file, bufferSize);
            return true;
        } catch (Exception e) {
            logger.error("Copy data from InputStream failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 将输入流中的数据转存至文件。
     * <p>
     * 简化方法，使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFile(InputStream, File, int)} 。
     *
     * @param stream 输入流。
     * @param file   目标文件。
     */
    public static boolean saveToFile(InputStream stream, File file) {
        return saveToFile(stream, file, BUFFER_SIZE_DEFAULT);
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
    public static void saveToFileUnsafe(ByteArrayOutputStream stream, File file, int bufferSize) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(stream.toByteArray());
        saveToFileUnsafe(input, file, bufferSize);
    }

    /**
     * 将 {@link ByteArrayOutputStream} 中的数据转存至文件。
     * <p>
     * 简化方法，使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFileUnsafe(ByteArrayOutputStream, File, int)} 。
     *
     * @param stream {@link ByteArrayOutputStream}实例。
     * @param file   目标文件。
     */
    public static void saveToFileUnsafe(ByteArrayOutputStream stream, File file) throws IOException {
        saveToFileUnsafe(stream, file, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 将 {@link ByteArrayOutputStream} 中的数据转存至文件。
     *
     * @param stream     {@link ByteArrayOutputStream}实例。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     * @return {@code true} 表示操作成功；{@code false} 表示操作失败。
     */
    public static boolean saveToFile(ByteArrayOutputStream stream, File file, int bufferSize) {
        try {
            saveToFileUnsafe(stream, file, bufferSize);
            return true;
        } catch (Exception e) {
            logger.error("Copy data from ByteArrayOutputStream failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 将 {@link ByteArrayOutputStream} 中的数据转存至文件。
     * <p>
     * 简化方法，使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFile(ByteArrayOutputStream, File, int)} 。
     *
     * @param stream {@link ByteArrayOutputStream}实例。
     * @param file   目标文件。
     */
    public static boolean saveToFile(ByteArrayOutputStream stream, File file) {
        return saveToFile(stream, file, BUFFER_SIZE_DEFAULT);
    }


    /*
     * ----- 将字节数组转存至文件 -----
     */

    /**
     * 将字节数组转存至文件。
     *
     * @param data       字节数组。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     */
    public static void saveToFileUnsafe(byte[] data, File file, int bufferSize) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        saveToFileUnsafe(input, file, bufferSize);
    }

    /**
     * 将字节数组中的数据转存至文件。
     * <p>
     * 简化方法，使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFileUnsafe(byte[], File, int)} 。
     *
     * @param data 字节数组。
     * @param file 目标文件。
     */
    public static void saveToFileUnsafe(byte[] data, File file) throws IOException {
        saveToFileUnsafe(data, file, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 将字节数组中的数据转存至文件。
     *
     * @param data       字节数组。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     */
    public static boolean saveToFile(byte[] data, File file, int bufferSize) {
        try {
            saveToFileUnsafe(data, file, bufferSize);
            return true;
        } catch (Exception e) {
            logger.error("Copy data from byte array failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 将字节数组中的数据转存至文件。
     * <p>
     * 简化方法，使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFile(byte[], File, int)} 。
     *
     * @param data 字节数组。
     * @param file 目标文件。
     */
    public static boolean saveToFile(byte[] data, File file) {
        return saveToFile(data, file, BUFFER_SIZE_DEFAULT);
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
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static void saveToFileUnsafe(FileDescriptor fd, File file, int bufferSize) throws IOException {
        // 校验输入参数的合法性
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must > 0!");
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
        }
    }

    /**
     * 将文件描述符指向的内容转存至文件。
     * <p>
     * 简化方法，默认使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFileUnsafe(FileDescriptor, File, int)} 。
     *
     * @param fd   文件描述符。
     * @param file 目标文件。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static void saveToFileUnsafe(FileDescriptor fd, File file) throws IOException {
        saveToFileUnsafe(fd, file, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 将文件描述符指向的内容转存至文件。
     *
     * @param fd         文件描述符。
     * @param file       目标文件。
     * @param bufferSize 缓冲区大小（字节）。
     * @return {@code true} 表示操作成功；{@code false} 表示操作失败。
     */
    public static boolean saveToFile(FileDescriptor fd, File file, int bufferSize) {
        try {
            saveToFileUnsafe(fd, file, bufferSize);
            return true;
        } catch (Exception e) {
            logger.error("Copy data from FileDescriptor failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 将文件描述符指向的内容转存至文件。
     * <p>
     * 简化方法，默认使用 {@code BUFFER_SIZE_DEFAULT} 缓冲区。其他行为详见 {@link #saveToFile(FileDescriptor, File, int)} 。
     *
     * @param fd   文件描述符。
     * @param file 目标文件。
     * @return {@code true} 表示操作成功；{@code false} 表示操作失败。
     */
    public static boolean saveToFile(FileDescriptor fd, File file) {
        return saveToFile(fd, file, BUFFER_SIZE_DEFAULT);
    }
}
