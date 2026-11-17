package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * Activity 信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class ActivityInfo(

    /**
     * 所在屏幕的逻辑 ID 。
     */
    @get:JvmName("getDisplayID")
    val displayID: String,

    /**
     * 包名。
     */
    @get:JvmName("getPackageName")
    val packageName: String,

    /**
     * 完整类名。
     */
    @get:JvmName("getClassName")
    val className: String,

    /**
     * 任务 ID 。若解析失败则为 `-1` 。
     */
    @get:JvmName("getTaskID")
    val taskID: Int = -1,

    /**
     * 用户 ID 。若解析失败则为 `-1` 。
     */
    @get:JvmName("getUserID")
    val userID: Int = -1,

    /**
     * 所属设备的序列号。
     */
    @get:JvmName("getSN")
    val sn: String = ""
) {

    /**
     * 获取组件名称，格式为 `包名/类名` 。
     *
     * @return 组件名称。
     */
    fun getComponentName(): String = "$packageName/$className"
}
