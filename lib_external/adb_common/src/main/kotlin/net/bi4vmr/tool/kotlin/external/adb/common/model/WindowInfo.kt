package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * Window 信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class WindowInfo(

    /**
     * 窗口标题。
     */
    @get:JvmName("getTitle")
    val title: String,

    /**
     * 所属软件包名。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getPackageName")
    val packageName: String = "",

    /**
     * 屏幕 ID。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getDisplayID")
    val displayID: String = "",

    /**
     * 用户 ID。
     *
     * 若解析失败则为 `-1`。
     */
    @get:JvmName("getUserID")
    val userID: Int = -1,

    /**
     * 窗口是否可见。
     *
     * 若解析失败也视为不可见。
     */
    @get:JvmName("isVisible")
    val visible: Boolean = false,

    /**
     * Window 的 Hash 值。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getHash")
    val hash: String = ""
)
