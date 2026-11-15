package net.bi4vmr.tool.java.common.base.file;

/**
 * 文件路径类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public enum FilePathType {

    /**
     * Unix 文件路径。
     * <p>
     * 格式为 {@code /home/bi4vmr/test.txt}。
     */
    UNIX,

    /**
     * Windows 文件路径。
     * <p>
     * 格式为 {@code D:\Data\test.txt}。
     */
    WINDOWS,

    /**
     * Windows Universal Naming Convention 路径。
     * <p>
     * 格式为 {@code \\PC_Home\Share\test.txt}，通常表示网络资源路径。
     */
    UNC,

    /**
     * 相对路径。
     * <p>
     * 可能是 Windows 或 Unix 平台的路径，或包含两种分隔符。
     */
    RELATIVE
}
