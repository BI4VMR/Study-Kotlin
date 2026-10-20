package net.bi4vmr.tool.kotlin.external.adb.model

import com.android.ddmlib.IDevice
import net.bi4vmr.tool.kotlin.external.adb.common.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.util.ADBUtil
import java.io.File

/**
 * ADB 设备。
 *
 * [getSN] 、 [getState] 、 [isOnline] 方法为内存读写，不必在子线程调用；其他方法耗时可能较长，建议在非 UI 线程调用。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class ADBDevice(

    /**
     * 设备状态。
     */
    private val ddmState: IDevice.DeviceState,

    /**
     * DDMLib 的 Device 实例。
     */
    val ddmDevice: IDevice
) {

    /*
     * ----- 连接信息 -----
     */

    /**
     * 获取序列号。
     *
     * @return ADB设备序列号。
     */
    fun getSN(): String = ddmDevice.serialNumber

    /**
     * 获取设备状态。
     *
     * @return 枚举常量。
     */
    fun getState(): DeviceState {
        return when (ddmState) {
            IDevice.DeviceState.OFFLINE -> DeviceState.OFFLINE
            IDevice.DeviceState.ONLINE -> DeviceState.ONLINE
            IDevice.DeviceState.RECOVERY -> DeviceState.RECOVERY
            IDevice.DeviceState.BOOTLOADER -> DeviceState.BOOTLOADER
        }
    }

    /**
     * 判断设备是否在线。
     *
     * 判断设备是否为 [DeviceState.ONLINE] 状态。
     *
     * @return `true` 表示在线， `false` 表示离线。
     */
    fun isOnline(): Boolean = ddmDevice.isOnline


    /*
     * ----- 常用命令 -----
     */

    /**
     * 执行 ADB Shell 命令。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否重定向错误输出流。默认值为 `true` 。
     * @return [Process] 对象，若执行出错则返回空值。
     */
    @JvmOverloads
    fun run(command: String, redirectError: Boolean = true): Process? =
        ADBUtil.run(getSN(), command, redirectError)

    /**
     * 执行 ADB Shell 命令并获取返回值。
     *
     * @param[command] 命令语句。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    fun runForStatus(command: String): Int =
        ADBUtil.runForStatus(getSN(), command)

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmOverloads
    fun runForLines(
        command: String,
        redirectError: Boolean = true,
        charset: String? = null
    ): List<String>? =
        ADBUtil.runForLines(getSN(), command, redirectError, charset)

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmOverloads
    fun runForText(command: String, redirectError: Boolean = true, charset: String? = null): String? =
        ADBUtil.runForText(getSN(), command, redirectError, charset)

    /**
     * 获取显示屏列表。
     *
     * @return 显示屏列表。
     */
    fun getDisplays(): List<DisplayInfo> = ADBUtil.getDisplays(getSN())

    /**
     * 获取显示屏物理 ID 索引。
     *
     * @param[displayID] 逻辑 ID 。
     * @return 索引序号。无法获取时返回 `0` 。
     */
    fun getDisplayUniqueIndex(displayID: String): String = ADBUtil.getDisplayUniqueIndex(getSN(), displayID)

    /**
     * 获取屏幕截图。
     *
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID 。
     * @return 二进制数据。若出现错误则返回空值。
     */
    @JvmOverloads
    fun screenshotAsPNG(displayID: String? = null): ByteArray? =
        ADBUtil.screenshotAsPNG(getSN(), displayID)

    /**
     * 获取屏幕截图并保存至 PC 存储设备。
     *
     * @param[file] 目标文件。
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID 。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    @JvmOverloads
    fun screenshotAsPNG(file: File, displayID: String? = null): Boolean =
        ADBUtil.screenshotAsPNG(getSN(), file, displayID)

    /**
     * 切换为 Root 权限。
     *
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    fun root(): Int = ADBUtil.root(getSN())

    /**
     * 重启。
     *
     * @param[option] 附加命令，可以携带 `recovery` 、 `fastboot` 、 `bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmOverloads
    fun reboot(option: String = ""): Int = ADBUtil.reboot(getSN(), option)


    /*
     * ----- 设备信息 -----
     */

    /**
     * 获取设备属性。
     *
     * @param[key] 属性键名。
     * @return 属性值。若属性不存在则返回空值。
     */
    fun getProperty(key: String): String? = ADBUtil.getProperty(getSN(), key)


    /**
     * 获取设备属性。
     *
     * @param[key] 属性键名。
     * @param[default] 默认值。
     * @return 属性值。若属性不存在则返回默认值。
     */
    fun getProperty(key: String, default: String): String =
        ADBUtil.getProperty(getSN(), key, default)

    /**
     * 获取设备制造商。
     *
     * @return 制造商。若设备未声明则为空字符串。
     */
    fun getManufacturer(): String = ADBUtil.getManufacturer(getSN())

    /**
     * 获取设备品牌。
     *
     * @return 品牌。若设备未声明则为空字符串。
     */
    fun getBrand(): String = ADBUtil.getBrand(getSN())

    /**
     * 获取设备型号。
     *
     * @return 型号。若设备未声明则为空字符串。
     */
    fun getModel(): String = ADBUtil.getModel(getSN())

    /**
     * 获取设备 Android 系统版本。
     *
     * @return 系统版本号。若设备未声明则为空字符串。
     */
    fun getOSVersion(): String = ADBUtil.getOSVersion(getSN())

    /**
     * 获取设备 Android API Level。
     *
     * @return API Level。若设备未声明则为空字符串。
     */
    fun getAPILevel(): String = ADBUtil.getAPILevel(getSN())

    /**
     * 获取设备名称。
     *
     * 若为物理机则显示 `<品牌> <型号>` ；若为模拟器则显示模拟器名称。
     *
     * 该方法耗时平均值为 `200` 毫秒左右，请勿在 UI 线程中调用。
     *
     * @param[default] 获取失败时的名称。默认值为“设备不可用”。
     * @return 设备名称。
     */
    @JvmOverloads
    fun getName(default: String = "设备不可用"): String {
        if (ddmDevice.isEmulator) {
            return ddmDevice.avdName
        }

        val name = "${getBrand()} ${getModel()}"
        return if (name.trim().isBlank()) default else name
    }


    /*
     * ----- 文件系统 -----
     */

    /**
     * 推送文件到设备。
     *
     * @param[src] PC 上的源文件。
     * @param[remote] 设备上的目标路径。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun pushFile(src: File, remote: String): Boolean = ADBUtil.pushFile(getSN(), src, remote)

    /**
     * 同步文件系统。
     *
     * 部分设备写入文件后需执行此命令，否则其他进程可能读取到不完整的数据。
     */
    fun syncFileSystem() = ADBUtil.syncFileSystem(getSN())

    /**
     * 重新挂载文件系统为可读写。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun remount(): Boolean = ADBUtil.remount(getSN())


    /*
     * ----- 网络通信 -----
     */

    /**
     * 开启 TCP/IP 远程控制。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun startRemoteControl(): Boolean = ADBUtil.startRemoteControl(getSN())

    /**
     * 关闭 TCP/IP 远程控制。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun stopRemoteControl(): Boolean = ADBUtil.stopRemoteControl(getSN())

    /**
     * 开启 ADB 端口转发（TCP）。
     *
     * @param[pcPort] PC 端口。
     * @param[devicePort] 设备端口。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun startForward(pcPort: Int, devicePort: Int): Boolean =
        ADBUtil.startForward(getSN(), pcPort, devicePort)

    /**
     * 关闭指定的 ADB 端口转发（TCP）。
     *
     * @param[pcPort] PC 端口。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun stopForward(pcPort: Int): Boolean = ADBUtil.stopForward(getSN(), pcPort)

    /**
     * 开启 ADB 端口转发（通用端点格式）。
     *
     * @param[pcEndPoint] PC 端点（如 `tcp:8080`、`localabstract:xxx`）。
     * @param[deviceEndPoint] 设备端点。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun startForward(pcEndPoint: String, deviceEndPoint: String): Boolean =
        ADBUtil.startForward(getSN(), pcEndPoint, deviceEndPoint)

    /**
     * 关闭指定的 ADB 端口转发（通用端点格式）。
     *
     * @param[pcEndPoint] PC 端点。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun stopForward(pcEndPoint: String): Boolean = ADBUtil.stopForward(getSN(), pcEndPoint)

    /**
     * 清除当前设备的所有 ADB 端口转发。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun cleanForward(): Boolean = ADBUtil.cleanForward(getSN())
}
