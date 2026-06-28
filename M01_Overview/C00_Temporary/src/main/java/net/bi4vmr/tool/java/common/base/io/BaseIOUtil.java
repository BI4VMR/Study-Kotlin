package net.bi4vmr.tool.java.common.base.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 基本输入与输出工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class BaseIOUtil extends IOUtil {

    /*
     * ----- 从输入流读取文本 -----
     */

    /**
     * 从输入流读取所有数据并转为行数组。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时会抛出异常。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @param charset    字符集。
     * @return 行数组。
     * @throws IllegalArgumentException 参数验证失败。
     * @throws IOException              发生IO错误。
     */
    public static List<String> readAsLinesUnsafe(InputStream stream, int bufferSize, Charset charset) throws IOException {
        // 校验输入参数
        if (stream == null) {
            throw new IllegalArgumentException("Param [stream] can not be null!");
        }

        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Param [bufferSize] must > 0!");
        }

        List<String> lines = new ArrayList<>();
        try (
                InputStreamReader streamReader = new InputStreamReader(stream, charset);
                BufferedReader reader = new BufferedReader(streamReader, bufferSize)
        ) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                lines.add(line);
            }

            return lines;
        }
    }

    public static List<String> readAsLinesUnsafe(InputStream stream, int bufferSize) throws IOException {
        return readAsLinesUnsafe(stream, bufferSize, StandardCharsets.UTF_8);
    }

    public static List<String> readAsLinesUnsafe(InputStream stream, Charset charset) throws IOException {
        return readAsLinesUnsafe(stream, BUFFER_SIZE_DEFAULT, charset);
    }

    public static List<String> readAsLinesUnsafe(InputStream stream) throws IOException {
        return readAsLinesUnsafe(stream, BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }

    public static List<String> readAsLines(InputStream stream, int bufferSize, Charset charset) {
        try {
            return readAsLinesUnsafe(stream, bufferSize, charset);
        } catch (Exception e) {
            System.err.println("BaseIOUtil: Read file as lines failed! Reason:[" + e.getMessage() + "]");
            return new ArrayList<>();
        }
    }

    public static List<String> readAsLines(InputStream stream, int bufferSize) {
        return readAsLines(stream, bufferSize, StandardCharsets.UTF_8);
    }

    public static List<String> readAsLines(InputStream stream, Charset charset) {
        return readAsLines(stream, BUFFER_SIZE_DEFAULT, charset);
    }

    public static List<String> readAsLines(InputStream stream) {
        return readAsLines(stream, BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }

    /**
     * 从输入流读取所有数据并转为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回空字符串。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @param charset    字符集。
     * @return 文本内容。
     */
    public static String readAsText(InputStream stream, int bufferSize, Charset charset) {
        // 校验输入参数
        if (stream == null) {
            System.err.println("BaseIOUtil - InputStream is null!");
            return "";
        }

        if (bufferSize <= 0) {
            System.err.println("BaseIOUtil - Buffer size must > 0!");
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (
                InputStreamReader streamReader = new InputStreamReader(stream, charset);
                BufferedReader reader = new BufferedReader(streamReader, bufferSize)
        ) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            System.err.println("BaseIOUtil - Read file as text failed! Reason:[" + e.getMessage() + "]");
        }

        return "";
    }

    /**
     * 从输入流读取所有数据并转为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回空字符串；默认使用"UTF-8"编码。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @return 文本内容。
     */
    public static String readAsText(InputStream stream, int bufferSize) {
        return readAsText(stream, bufferSize, StandardCharsets.UTF_8);
    }

    /**
     * 从输入流读取所有数据并转为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回空字符串，默认使用"8KB"缓冲区。
     *
     * @param stream  输入流。
     * @param charset 字符集。
     * @return 文本内容。
     */
    public static String readAsText(InputStream stream, Charset charset) {
        return readAsText(stream, BUFFER_SIZE_DEFAULT, charset);
    }

    /**
     * 从输入流读取所有数据并转为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回空字符串；默认使用"UTF-8"编码及"8KB"缓冲区。
     *
     * @param stream 输入流。
     * @return 文本内容。
     */
    public static String readAsText(InputStream stream) {
        return readAsText(stream, BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }


    /*
     * ----- 从输入流读取二进制数据 -----
     */

    /**
     * 从输入流读取二进制数据。
     * <p>
     * 该方法将从第二参数"offset"指定位置开始，读取第三参数"length"指定长度的数据。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream     输入流。
     * @param offset     起始位置（从0开始计数）。
     * @param length     读取字节数。
     * @param bufferSize 缓冲区大小（字节）。
     * @return 字节数组。
     */
    public static byte[] readAsBytes(InputStream stream, long offset, int length, int bufferSize) {
        // 校验输入参数
        if (stream == null) {
            System.err.println("BaseIOUtil - InputStream is null!");
            return new byte[0];
        }

        if (offset < 0L || length <= 0 || bufferSize <= 0) {
            System.err.println("BaseIOUtil - Offset or length value invalid!");
            return new byte[0];
        }

        try (
                BufferedInputStream bis = new BufferedInputStream(stream, bufferSize)
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

            List<byte[]> result = new ArrayList<>();
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
                    result.add(Arrays.copyOfRange(buffer, 0, count));
                    // 更新剩余的数据量
                    remaining -= count;
                }
            }

            // 将每轮读取到的数据合并为单个数组
            return mergeByteArrayList(result);
        } catch (IOException e) {
            System.err.println("BaseIOUtil - Read file as bytes failed! Reason:[" + e.getMessage() + "]");
        }

        return new byte[0];
    }

    /**
     * 从输入流读取二进制数据。
     * <p>
     * 该方法将从第二参数"offset"指定位置开始，读取第三参数"length"指定长度的数据。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回内容为空的数组；默认使用"8KB"缓冲区。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream 输入流。
     * @param offset 起始位置（从0开始计数）。
     * @param length 读取字节数。
     * @return 字节数组。遇到错误时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(InputStream stream, long offset, int length) {
        return readAsBytes(stream, offset, length, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 从输入流读取二进制数据。
     * <p>
     * 该方法将读取第二参数"length"指定长度的数据。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回内容为空的数组；默认使用"8KB"缓冲区。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream 输入流。
     * @param length 读取字节数。
     * @return 字节数组。遇到错误时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(InputStream stream, int length) {
        return readAsBytes(stream, 0L, length, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 从输入流读取所有二进制数据。
     * <p>
     * 操作完毕后输入流将被关闭；遇到异常时默认返回内容为空的数组；默认使用"8KB"缓冲区。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过2GiB的部分。这是因为数组容量受到"int"类型最大值的限制，并且单次读取过多数据也可能导
     * 致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream 输入流。
     * @return 字节数组。遇到错误时将返回内容为空的数组。
     */
    public static byte[] readAsBytes(InputStream stream) {
        return readAsBytes(stream, 0L, Integer.MAX_VALUE, BUFFER_SIZE_DEFAULT);
    }


    /**
     * 将Byte数组List中的元素合并为一个Byte数组。
     *
     * @param input 待合并的List。
     * @return 合并后的Byte数组。
     */
    private static byte[] mergeByteArrayList(List<byte[]> input) {
        // 校验输入参数
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }

        // 计算元素总数并过滤空值
        int validCount = 0;
        List<byte[]> listNonNull = new ArrayList<>();
        for (byte[] item : input) {
            if (item != null) {
                validCount += item.length;
                listNonNull.add(item);
            }
        }

        // 合并结果
        byte[] result = new byte[validCount];
        int position = 0;
        for (byte[] item : listNonNull) {
            System.arraycopy(item, 0, result, position, item.length);
            // 这里是当前数组的长度，比Index多一位，因此下一轮会从有效数据后一位开始填充，无需手动+1。
            position += item.length;
        }

        return result;
    }
}
