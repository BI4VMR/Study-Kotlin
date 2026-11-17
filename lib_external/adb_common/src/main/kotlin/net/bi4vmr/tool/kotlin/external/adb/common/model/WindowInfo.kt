package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * 窗口信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class WindowInfo(

    /**
     * 窗口对象的哈希值，即 `Window{xxx ...}` 中的第一项。
     */
    @get:JvmName("getHash")
    val hash: String,

    /**
     * 窗口标题。
     *
     * 对于 Activity 窗口通常为 `包名/类名` 格式；对于其他窗口则为自定义名称，例如 `NavigationBar0` 。
     */
    @get:JvmName("getTitle")
    val title: String,

    /**
     * 所在屏幕的逻辑 ID 。若解析失败则为空字符串。
     */
    @get:JvmName("getDisplayID")
    val displayID: String = "",

    /**
     * 所属软件包名。若解析失败则为空字符串。
     */
    @get:JvmName("getPackageName")
    val packageName: String = "",

    /**
     * 用户 ID 。若解析失败则为 `-1` 。
     */
    @get:JvmName("getUserID")
    val userID: Int = -1,

    /**
     * 是否可见。
     */
    @get:JvmName("isVisible")
    val visible: Boolean = false,

    /**
     * 所属设备的序列号。
     */
    @get:JvmName("getSN")
    val sn: String = ""
)
