package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo

/**
 * ADB 执行器公共能力接口。
 *
 * 本地与远程执行器通过该接口向上层提供统一的设备操作能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBExecutor {

    fun getType(): ExecutorType

    /**
     * 初始化，可重复调用。用于首次连接，或初始化失败、连接断开后重试。
     */
    fun init() {}

    /**
     * 释放资源，可重复调用。释放后可再次调用 [init] 重新连接。
     */
    fun release() {}

    fun getDisplayName(): String

    /**
     * Flow : 执行器状态。
     */
    fun getStateFlow(): StateFlow<ServiceState>

    /**
     * Flow : 初始化错误信息。
     */
    fun getExceptionFlow(): StateFlow<Exception?>

    /**
     * Flow : 设备列表。
     */
    fun getDevicesFlow(): StateFlow<List<ADBDevice>>

    /**
     * 获取设备的显示屏信息列表。
     *
     * @param[sn] 设备序列号。
     * @return 显示屏信息列表，若出现异常则返回空列表。
     */
    suspend fun getDisplays(sn: String): List<DisplayInfo>

    /**
     * 在设备上执行 ADB 命令并获取文本输出。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句，默认以 `adb shell` 方式执行。
     * @return 命令输出文本，若出现异常则返回空值。
     */
    suspend fun runForText(sn: String, command: String): String?

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
     * 若为物理机则显示 `<品牌> <型号>` ；若为模拟器则显示模拟器名称。
     *
     * @param[sn] 设备序列号。
     * @param[fallback] 获取失败时的名称。
     * @return 设备名称。
     */
    suspend fun getName(sn: String, fallback: String): String

    /**
     * 获取设备类型。
     *
     * 为避免与执行器自身的 [getType] 混淆，此处命名为 `getDeviceType` 。
     *
     * @param[sn] 设备序列号。
     * @return 设备类型。若设备不存在或出现异常则返回空值。
     */
    suspend fun getDeviceType(sn: String): DeviceType?

    fun openShell(sn: String)
}
