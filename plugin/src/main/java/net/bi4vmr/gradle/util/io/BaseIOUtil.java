package net.bi4vmr.gradle.util.io;

import net.bi4vmr.gradle.util.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(BaseIOUtil.class);


    /*
     * ----- 从输入流读取文本 -----
     */

    /**
     * 将输入流读取为分行文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @param charset    字符集。
     * @return 分行文本。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static List<String> readAsLinesUnsafe(InputStream stream, int bufferSize, Charset charset) throws IOException {
        /* 校验输入参数 */
        if (stream == null) {
            throw new IllegalArgumentException("Param 'stream' can not be null!");
        }

        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Param 'bufferSize' must > 0, but got [" + bufferSize + "]!");
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

    /**
     * 将输入流读取为分行文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     * <p>
     * 简化方法，默认使用 {@code 8KB} 缓冲区与 {@code UTF-8} 编码；其他行为详见
     * {@link #readAsLinesUnsafe(InputStream, int, Charset)} 。
     *
     * @param stream 输入流。
     * @return 分行文本。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static List<String> readAsLinesUnsafe(InputStream stream) throws IOException {
        return readAsLinesUnsafe(stream, BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }

    /**
     * 将输入流读取为分行文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @param charset    字符集。
     * @return 分行文本。读取失败时为空值。
     */
    public static List<String> readAsLines(InputStream stream, int bufferSize, Charset charset) {
        try {
            return readAsLinesUnsafe(stream, bufferSize, charset);
        } catch (Exception e) {
            logger.error("Read file as lines failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将输入流读取为分行文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     * <p>
     * 简化方法，默认使用 {@code 8KB} 缓冲区与 {@code UTF-8} 编码；其他行为详见
     * {@link #readAsLines(InputStream, int, Charset)} 。
     *
     * @param stream 输入流。
     * @return 分行文本。读取失败时为空值。
     */
    public static List<String> readAsLines(InputStream stream) {
        return readAsLines(stream, BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }

    /**
     * 将输入流读取为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @param charset    字符集。
     * @return 文本内容。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static String readAsTextUnsafe(InputStream stream, int bufferSize, Charset charset) throws IOException {
        List<String> lines = readAsLinesUnsafe(stream, bufferSize, charset);
        return String.join(System.lineSeparator(), lines);
    }

    /**
     * 将输入流读取为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     * <p>
     * 简化方法，默认使用 {@code 8KB} 缓冲区与 {@code UTF-8} 编码；其他行为详见 {@link #readAsTextUnsafe(InputStream, int, Charset)} 。
     *
     * @param stream 输入流。
     * @return 文本内容。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static String readAsTextUnsafe(InputStream stream) throws IOException {
        return readAsTextUnsafe(stream, IOUtil.BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }

    /**
     * 将输入流读取为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     *
     * @param stream     输入流。
     * @param bufferSize 缓冲区大小（字节）。
     * @param charset    字符集。
     * @return 文本内容。读取失败时为空值。
     */
    public static String readAsText(InputStream stream, int bufferSize, Charset charset) {
        try {
            return readAsTextUnsafe(stream, bufferSize, charset);
        } catch (Exception e) {
            logger.error("Read file as text failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
        }

        return null;
    }

    /**
     * 将输入流读取为文本。
     * <p>
     * 一次性读取流中的所有数据，只适合数据量已知且较小的流，数据量过大可能会导致内存溢出。操作完毕后输入流将被关闭。
     * <p>
     * 简化方法，默认使用 {@code 8KB} 缓冲区与 {@code UTF-8} 编码；其他行为详见 {@link #readAsText(InputStream, int, Charset)} 。
     *
     * @param stream 输入流。
     * @return 文本内容。读取失败时为空值。
     */
    public static String readAsText(InputStream stream) {
        return readAsText(stream, BUFFER_SIZE_DEFAULT, StandardCharsets.UTF_8);
    }


    /*
     * ----- 从输入流读取二进制数据 -----
     */

    /**
     * 将输入流读取为二进制数据。
     * <p>
     * 该方法将从第二参数 {@code offset} 指定位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过 `2GiB` 的部分。这是因为数组容量受到 {@link Integer#MAX_VALUE} 类型最大值的限制，
     * 并且单次读取过多数据也可能导致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream     输入流。
     * @param offset     起始位置（从 `0` 开始计数）。
     * @param length     读取字节数。
     * @param bufferSize 缓冲区大小（字节）。
     * @return 字节数组。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static byte[] readAsBytesUnsafe(InputStream stream, long offset, int length, int bufferSize)
            throws IOException {
        /* 校验输入参数 */
        if (stream == null) {
            throw new IllegalArgumentException("Param 'stream' can not be null!");
        }
        if (offset < 0L) {
            throw new IllegalArgumentException("Param 'offset' must >= 0, but got [" + offset + "]!");
        }
        if (length <= 0L) {
            throw new IllegalArgumentException("Param 'length' must > 0, but got [" + length + "]!");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Param 'bufferSize' must > 0, but got [" + bufferSize + "]!");
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
        }
    }

    /**
     * 将输入流读取为二进制数据。
     * <p>
     * 简化方法，默认从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据，使用 {@code 8KB} 缓冲区；其他行为详见
     * {@link #readAsBytesUnsafe(InputStream, long, int, int)} 。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过 `2GiB` 的部分。这是因为数组容量受到 {@link Integer#MAX_VALUE} 类型最大值的限制，
     * 并且单次读取过多数据也可能导致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream 输入流。
     * @return 字节数组。
     * @throws IOException              发生I/O错误。
     * @throws IllegalArgumentException 参数验证失败。
     */
    public static byte[] readAsBytesUnsafe(InputStream stream) throws IOException {
        return readAsBytesUnsafe(stream, 0L, Integer.MAX_VALUE, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 将输入流读取为二进制数据。
     * <p>
     * 该方法将从第二参数 {@code offset} 指定位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过 `2GiB` 的部分。这是因为数组容量受到 {@link Integer#MAX_VALUE} 类型最大值的限制，
     * 并且单次读取过多数据也可能导致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     *
     * @param stream     输入流。
     * @param offset     起始位置（从 `0` 开始计数）。
     * @param length     读取字节数。
     * @param bufferSize 缓冲区大小（字节）。
     * @return 字节数组。读取失败时为空值。
     */
    public static byte[] readAsBytes(InputStream stream, long offset, int length, int bufferSize) {
        try {
            return readAsBytesUnsafe(stream, offset, length, bufferSize);
        } catch (Exception e) {
            logger.error("Read file as bytes failed! Reason:[{}: {}]", e.getClass().getSimpleName(), e.getMessage());
        }

        return null;
    }

    /**
     * 将输入流读取为二进制数据。
     * <p>
     * 该方法将从第二参数 {@code offset} 指定位置开始，读取第三参数 {@code length} 指定长度的数据。
     * <p>
     * 该方法仅适用于简单数据的处理，无法处理长度超过 `2GiB` 的部分。这是因为数组容量受到 {@link Integer#MAX_VALUE} 类型最大值的限制，
     * 并且单次读取过多数据也可能导致内存溢出。对于大文件读取场景，调用者可以分块读取文件并进行处理。
     * <p>
     * 简化方法，默认从首字节开始，最多读取 {@code Integer.MAX_VALUE} 字节数据，使用 {@code 8KB} 缓冲区；其他行为详见
     * {@link #readAsBytesUnsafe(InputStream, long, int, int)} 。
     *
     * @param stream 输入流。
     * @return 字节数组。读取失败时为空值。
     */
    public static byte[] readAsBytes(InputStream stream) {
        return readAsBytes(stream, 0L, Integer.MAX_VALUE, BUFFER_SIZE_DEFAULT);
    }

    /**
     * 在子线程读取并丢弃数据。
     * <p>
     * 用于消耗无用但会导致阻塞的流，例如不关心输出时的 {@code Process} 流。
     *
     * @param stream 输入流。
     */
    public static void readAndDrop(InputStream stream) {
        new Thread(() -> {
            try (
                    BufferedInputStream bis = new BufferedInputStream(stream, BUFFER_SIZE_DEFAULT)
            ) {
                byte[] buffer = new byte[BUFFER_SIZE_DEFAULT];
                while (true) {
                    int count = bis.read(buffer);
                    // 如果读取方法返回负数，表示已到文件末尾。
                    if (count == -1) {
                        break;
                    }

                    // 未到结尾，继续读取直到消耗所有内容。
                }
            } catch (Exception e) {
                // 无需处理
            }
        }).start();
    }


    /*
     * ----- 内部方法 -----
     */

    /**
     * 将字节数组List中的元素合并为一个字节数组。
     *
     * @param input 待合并的List。
     * @return 合并后的字节数组。
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
