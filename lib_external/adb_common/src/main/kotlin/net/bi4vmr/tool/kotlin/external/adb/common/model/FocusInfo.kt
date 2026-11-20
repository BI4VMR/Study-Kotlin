package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * 界面焦点信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class FocusInfo(

    /**
     * 前台应用程序包名。
     *
     * 若设备未声明或解析失败则为 `-`。
     */
    @get:JvmName("getActivityPackage")
    val activityPackage: String = "-",

    /**
     * 前台 Activity 类名。
     *
     * 若设备未声明或解析失败则为 `-`。
     */
    @get:JvmName("getActivityClass")
    val activityClass: String = "-",

    /**
     * 前台 Activity Hash。
     *
     * 若设备未声明或解析失败则为 `-`。
     */
    @get:JvmName("getActivityHash")
    val activityHash: String = "-",

    /**
     * 前台 Window 名称。
     *
     * 若设备未声明或解析失败则为 `-`。
     */
    @get:JvmName("getWindowName")
    val windowName: String = "-",

    /**
     * 前台 Window Hash。
     *
     * 若设备未声明或解析失败则为 `-`。
     */
    @get:JvmName("getWindowHash")
    val windowHash: String = "-",

    /**
     * 屏幕 ID。
     *
     * 若设备未声明或解析失败则为 `-`。
     */
    @get:JvmName("getDisplayID")
    val displayID: String = "-"
) {

    /**
     * 前台 Activity 组件名称。
     */
    @get:JvmName("getComponentName")
    val componentName: String = "$activityPackage/$activityClass"
}
