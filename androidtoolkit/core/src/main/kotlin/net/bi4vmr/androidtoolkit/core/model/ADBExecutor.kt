package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.*

/**
 * ADB 执行器公共能力接口。
 *
 * 本地与远程执行器通过该接口向上层提供统一的设备操作能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBExecutor {

    /**
     * 初始化。
     */
    fun init() {}

    /**
     * 释放资源。
     *
     * 释放后可再次调用 [init] 重新连接。
     */
    fun release() {}

    /**
     * 获取执行器 ID。
     */
    fun getID(): String

    /**
     * 获取执行器类型。
     */
    fun getType(): ExecutorType

    /**
     * 获取执行器名称。
     */
    suspend fun getDisplayName(): String

    /**
     * Flow：执行器状态。
     */
    fun getStateFlow(): StateFlow<ServiceState>

    /**
     * Flow：错误信息。
     */
    fun getExceptionFlow(): StateFlow<Exception?>

    /**
     * Flow：设备列表。
     */
    fun getDevicesFlow(): StateFlow<List<ADBDevice>>


    /*
     * ----- 常用命令 -----
     */

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    suspend fun runForText(sn: String, command: String): String?

    fun openShell(sn: String)

    /**
     * 切换为 Root 权限。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    suspend fun root(sn: String): Int

    /**
     * 重启。
     *
     * @param[sn] 设备序列号。
     * @param[option] 附加命令，可以携带 `recovery`、`fastboot`、`bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    suspend fun reboot(sn: String, option: String = ""): Int


    /*
     * ----- 设备信息 -----
     */

    /**
     * 获取设备制造商。
     *
     * @param[sn] 设备序列号。
     * @return 制造商。若设备未声明则为空字符串。
     */
    suspend fun getManufacturer(sn: String): String

    /**
     * 获取设备品牌。
     *
     * @param[sn] 设备序列号。
     * @return 品牌。若设备未声明则为空字符串。
     */
    suspend fun getBrand(sn: String): String

    /**
     * 获取设备型号。
     *
     * @param[sn] 设备序列号。
     * @return 型号。若设备未声明则为空字符串。
     */
    suspend fun getModel(sn: String): String

    /**
     * 获取设备 Android 系统版本。
     *
     * @param[sn] 设备序列号。
     * @return 系统版本号。若设备未声明则为空字符串。
     */
    suspend fun getOSVersion(sn: String): String

    /**
     * 获取设备 Android API Level 。
     *
     * @param[sn] 设备序列号。
     * @return API Level 。若设备未声明则为空字符串。
     */
    suspend fun getAPILevel(sn: String): String

    /**
     * 获取设备名称。
     *
     * @param[sn] 设备序列号。
     * @param[fallback] 获取失败时的名称。
     * @return 设备名称。
     */
    suspend fun getDeviceName(sn: String, fallback: String): String

    /**
     * 获取设备类型。
     *
     * @param[sn] 设备序列号。
     * @return 设备类型。若设备不存在或出现异常则返回空值。
     */
    suspend fun getDeviceType(sn: String): ConnectionType?


    /*
     * ----- 图形界面 -----
     */

    /**
     * 获取设备的显示屏信息列表。
     *
     * @param[sn] 设备序列号。
     * @return 显示屏信息列表，若出现异常则返回空列表。
     */
    suspend fun getDisplays(sn: String): List<DisplayInfo>

    /**
     * 获取显示屏分辨率。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID 。为空值时表示默认显示屏。
     * @return 分辨率信息。若出现异常或显示屏不存在则返回空值。
     */
    suspend fun getDisplaySize(sn: String, displayID: String): DisplaySize?

    /**
     * 设置显示屏分辨率。
     *
     * @param[sn] 设备序列号。
     * @param[width] 宽度。
     * @param[height] 高度。
     * @param[displayID] 显示屏逻辑 ID 。为空值时表示默认显示屏。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    suspend fun setDisplaySize(sn: String, width: Int, height: Int, displayID: String): Boolean

    /**
     * 还原显示屏分辨率。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID 。为空值时表示默认显示屏。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    suspend fun resetDisplaySize(sn: String, displayID: String): Boolean

    /**
     * 获取显示屏像素密度。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID 。为空值时表示默认显示屏。
     * @return 密度信息。若出现异常则返回空值。
     */
    suspend fun getDisplayDensity(sn: String, displayID: String): DisplayDensity?

    /**
     * 设置显示屏像素密度。
     *
     * @param[sn] 设备序列号。
     * @param[density] 密度。
     * @param[displayID] 显示屏逻辑 ID 。为空值时表示默认显示屏。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    suspend fun setDisplayDensity(sn: String, density: Int, displayID: String): Boolean

    /**
     * 还原显示屏像素密度。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID 。为空值时表示默认显示屏。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    suspend fun resetDisplayDensity(sn: String, displayID: String): Boolean

    /**
     * 获取 Activity 列表。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID 。为空值时列出所有显示屏的 Activity 。
     * @return Activity 列表，若出现异常则返回空列表。
     */
    suspend fun getActivities(sn: String, displayID: String?): List<ActivityInfo>

    /**
     * 获取窗口列表。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID 。为空值时列出所有显示屏的窗口。
     * @return 窗口列表，若出现异常则返回空列表。
     */
    suspend fun getWindows(sn: String, displayID: String?): List<WindowInfo>

    /**
     * 获取全局焦点信息。
     *
     * Android 9 及更低版本多屏 API 不完善，焦点所在屏幕始终为 `0`。
     *
     * @param[sn] 设备序列号。
     * @return 全局焦点信息。
     */
    suspend fun getGlobalFocusInfo(sn: String): FocusInfo


    /*
     * ----- 文件系统 -----
     */

    /**
     * 获取目录下的文件列表。
     *
     * @param[sn] 设备序列号。
     * @param[path] 目录的绝对路径。
     * @return 文件列表。若路径不存在或不是目录则返回空值。
     */
    suspend fun listFiles(sn: String, path: String): List<DeviceFile>?


    /*
     * ----- 软件包管理 -----
     */

    /**
     * 获取软件包列表。
     *
     * @param[sn] 设备序列号。
     * @param[type] 类型筛选条件。
     * @return 包名列表。永不为空值，命令执行异常时返回空列表。
     */
    suspend fun listPackages(sn: String, type: PackageType): List<String>
}
