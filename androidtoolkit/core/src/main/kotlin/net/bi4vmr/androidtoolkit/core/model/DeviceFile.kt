package net.bi4vmr.androidtoolkit.core.model

/**
 * 设备文件节点。
 *
 * 与具体实现（DDMLib 、代理协议）无关的文件描述信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DeviceFile(

    /**
     * 文件名称。
     */
    val name: String,

    /**
     * 完整路径。
     */
    val path: String,

    /**
     * 是否为目录（包括指向目录的链接）。
     */
    val isDirectory: Boolean,

    /**
     * 是否为符号链接。
     */
    val isLink: Boolean,

    /**
     * 文件大小（字节）。
     */
    val size: Long,

    /**
     * 修改时间，格式为 `yyyy-MM-dd HH:mm` 。
     */
    val modifyTime: String,

    /**
     * 权限描述，例如 `drwxr-xr-x` 。
     */
    val permissions: String,

    /**
     * 链接目标。仅当 [isLink] 为真时有效。
     */
    val linkTarget: String? = null
)
