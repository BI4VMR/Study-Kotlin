package net.bi4vmr.tool.java.common.base.io;

import java.io.Closeable;

/**
 * 通用输入与输出工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class IOUtil {

    /**
     * 默认的缓冲区大小：8KB。
     */
    protected final static int BUFFER_SIZE_DEFAULT = 8 * 1024;

    /**
     * 常用缓冲区大小：8KB。
     */
    public final static int BUFFER_SIZE_8KB = 8 * 1024;

    /**
     * 常用缓冲区大小：128KB。
     */
    public final static int BUFFER_SIZE_128KB = 128 * 1024;

    /**
     * 常用缓冲区大小：256KB。
     */
    public final static int BUFFER_SIZE_256KB = 256 * 1024;

    /**
     * 常用缓冲区大小：512KB。
     */
    public final static int BUFFER_SIZE_512KB = 512 * 1024;

    /**
     * 常用缓冲区大小：1MB。
     */
    public final static int BUFFER_SIZE_1MB = 1024 * 1024;

    /**
     * 常用缓冲区大小：4MB。
     */
    public final static int BUFFER_SIZE_4MB = 4 * 1024 * 1024;

    /**
     * 常用缓冲区大小：8MB。
     */
    public final static int BUFFER_SIZE_8MB = 8 * 1024 * 1024;

    /**
     * 常用缓冲区大小：32MB。
     */
    public final static int BUFFER_SIZE_32MB = 32 * 1024 * 1024;


    /**
     * 释放资源。
     * <p>
     * 关闭实现了Closeable接口的资源，例如文件输入或输出流。
     * <p>
     * 如果遇到异常，会将错误信息输出到控制台上。
     *
     * @param closeable 需要关闭的Closeable资源。
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception e) {
            System.err.println("Close failed! Reason:[" + e.getMessage() + "]");
            e.printStackTrace();
        }
    }

    /**
     * 释放资源（静默）。
     * <p>
     * 关闭实现了Closeable接口的资源，例如文件输入或输出流。
     * <p>
     * 如果遇到异常，会将其忽略。
     *
     * @param closeable 需要关闭的Closeable资源。
     */
    public static void closeSilently(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception e) {
            // 静默关闭，无需进一步处理。
        }
    }
}
