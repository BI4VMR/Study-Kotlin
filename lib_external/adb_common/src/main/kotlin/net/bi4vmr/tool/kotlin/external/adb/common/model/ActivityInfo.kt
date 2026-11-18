package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * Activity 信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class ActivityInfo(

    /**
     * 包名。
     */
    @get:JvmName("getPackageName")
    val packageName: String,

    /**
     * 类名。
     */
    @get:JvmName("getClassName")
    val className: String,

    /**
     * 屏幕逻辑 ID。
     */
    @get:JvmName("getDisplayID")
    val displayID: String,

    /**
     * 任务 ID。
     *
     * 若解析失败则为 `-1`。
     */
    @get:JvmName("getTaskID")
    val taskID: Int = -1,

    /**
     * 用户 ID。
     *
     * 若解析失败则为 `-1`。
     */
    @get:JvmName("getUserID")
    val userID: Int = -1,

    /**
     * 进程 ID。
     *
     * 若解析失败则为 `-1`。
     */
    @get:JvmName("getProcessID")
    val processID: Int = -1,

    /**
     * 进程名称。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getProcessName")
    val processName: String = "",

    /**
     * 任务亲和性。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getTaskAffinity")
    val taskAffinity: String = "",

    /**
     * 启动 Intent。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getIntent")
    val intent: String = "",

    /**
     * 启动者的 UID。
     *
     * 若解析失败则为 `-1`。
     */
    @get:JvmName("getLaunchedFromUID")
    val launchedFromUID: Int = -1,

    /**
     * 启动者的包名。
     *
     * 若解析失败则为空字符串。
     */
    @get:JvmName("getLaunchedFromPackage")
    val launchedFromPackage: String = "",

    /**
     * 是否正在前台。
     *
     * TODO 暂未实现。
     */
    @get:JvmName("isResumed")
    val resumed: Boolean = false
) {

    /**
     * 组件名称。
     */
    @get:JvmName("getComponentName")
    val componentName: String = "$packageName/$className"
}
