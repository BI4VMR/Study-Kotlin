package net.bi4vmr.tool.kotlin.external.adb.common.util

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.io.BaseIOUtil
import net.bi4vmr.tool.java.common.base.io.FileIOUtil
import net.bi4vmr.tool.java.common.base.io.IOUtil
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper.getExecutableName
import java.io.File
import java.io.IOException

/**
 * ADB 命令行工具。
 *
 * 提供获取设备信息、执行命令等方法。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBCLIUtil {

    /**
     * 设备属性名称：制造商。
     */
    private const val PROPERTY_MANUFACTURER = "ro.product.manufacturer"

    /**
     * 设备属性名称：品牌。
     */
    private const val PROPERTY_BRAND = "ro.product.brand"

    /**
     * 设备属性名称：型号。
     */
    private const val PROPERTY_MODEL = "ro.product.model"

    /**
     * 设备属性名称： Android 版本。
     */
    private const val PROPERTY_OSVERSION = "ro.build.version.release"

    /**
     * 设备属性名称： Android API Level 。
     */
    private const val PROPERTY_APILEVEL = "ro.build.version.sdk"

    /**
     * 正则表达式： ADB 命令。
     */
    private val adbCMDRegex: Regex = """^adb\s+(?:-s\s+\S+\s+)?(shell|exec-out)\s*""".toRegex()


    /*
     * ----- 常用命令 -----
     */

    /**
     * 执行 ADB Shell 命令。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @param[redirectError] 是否重定向错误输出流。默认值为 `true` 。
     * @return [Process] 实例，永不为空，进程启动失败时抛出异常。
     * @throws[IOException] 进程启动失败。
     */
    @JvmOverloads
    @JvmStatic
    fun run(sn: String, command: String, redirectError: Boolean = true): Process {
        val optimizedCommand = buildCommand(sn, command)
        return CLIUtil.run(optimizedCommand, redirectError)
    }

    /**
     * 执行 ADB Shell 命令并获取返回值。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun runForStatus(sn: String, command: String): Int {
        val optimizedCommand = buildCommand(sn, command)
        return CLIUtil.runForStatus(optimizedCommand)
    }

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun runForLines(
        sn: String,
        command: String,
        redirectError: Boolean = true,
        charset: String? = null
    ): List<String>? {
        val optimizedCommand = buildCommand(sn, command)
        return CLIUtil.runForLines(optimizedCommand, redirectError, charset)
    }

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun runForText(sn: String, command: String, redirectError: Boolean = true, charset: String? = null): String? {
        val optimizedCommand = buildCommand(sn, command)
        return CLIUtil.runForText(optimizedCommand, redirectError, charset)
    }

    /**
     * 获取显示屏列表。
     *
     * @param[sn] 设备序列号。
     * @return 显示屏列表。
     */
    @JvmStatic
    fun getDisplays(sn: String): List<DisplayInfo> {
        // 部分 Android 11 设备也不支持新版命令，因此直接尝试调用新版命令，若未获取内容则回退到旧版命令。
        var lines: List<String> = runForLines(sn, "cmd display get-displays") ?: emptyList()
        var displays: List<DisplayInfo> = ADBOutputParser.parseDisplayInfoV11U(lines)
        if (displays.isEmpty()) {
            lines = runForLines(sn, "dumpsys display | grep 'mBaseDisplayInfo=DisplayInfo'") ?: emptyList()
            displays = ADBOutputParser.parseDisplayInfoV10(lines)

            // 如果这个方法也没解析到内容，继续回退到旧版本对应的方法。
            if (displays.isEmpty()) {
                displays = ADBOutputParser.parseDisplayInfoV9L(lines)
            }
        }

        // 填充设备序号并返回
        return displays.map { it.copy(sn = sn) }
    }

    /**
     * 获取显示屏物理 ID 索引。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 逻辑 ID 。
     * @return 索引序号。无法获取时返回 `0` 。
     */
    @JvmStatic
    fun getDisplayUniqueIndex(sn: String, displayID: String): String {
        // 对于 Android 9 及更低版本可能解析不到数据，此时默认返回 `0` 。
        return getDisplays(sn)
            .find { it.id == displayID }
            ?.uniqueID
            ?: "0"
    }

    /**
     * 获取屏幕截图。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID 。
     * @return 二进制数据。若出现错误则返回空值。
     */
    @JvmOverloads
    @JvmStatic
    fun screenshotAsPNG(sn: String, displayID: String? = null): ByteArray? {
        try {
            val cmd = if (displayID == null) {
                String.format("${getExecutableName()} -s %s exec-out screencap -p", sn)
            } else {
                val physicalID = getDisplayUniqueIndex(sn, displayID)
                String.format("${getExecutableName()} -s %s exec-out screencap -d %s -p", sn, physicalID)
            }

            val process = CLIUtil.run(cmd, false)
            // 进程启动失败应当抛出异常，正常情况下不会执行至此处。
            requireNotNull(process) { "Unreachable code." }
            // 丢弃错误输出，防止阻塞其他线程。
            BaseIOUtil.readAndDrop(process.errorStream)
            return BaseIOUtil.readAsBytes(
                process.inputStream,
                0,
                Int.MAX_VALUE,
                IOUtil.BUFFER_SIZE_8MB
            )
        } catch (_: Exception) {
            return null
        }
    }


    /**
     * 获取屏幕截图并保存至 PC 存储设备。
     *
     * @param[sn] 设备序列号。
     * @param[file] 目标文件。
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID 。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    @JvmOverloads
    @JvmStatic
    fun screenshotAsPNG(sn: String, file: File, displayID: String? = null): Boolean {
        try {
            val cmd = if (displayID == null) {
                String.format("${getExecutableName()} -s %s exec-out screencap -p", sn)
            } else {
                val physicalID = getDisplayUniqueIndex(sn, displayID)
                String.format("${getExecutableName()} -s %s exec-out screencap -d %s -p", sn, physicalID)
            }

            val process = CLIUtil.run(cmd, false)
            // 进程启动失败应当抛出异常，正常情况下不会执行至此处。
            requireNotNull(process) { "Unreachable code." }
            // 丢弃错误输出，防止阻塞其他线程。
            BaseIOUtil.readAndDrop(process.errorStream)
            return FileIOUtil.saveToFile(process.inputStream, file, IOUtil.BUFFER_SIZE_8MB)
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * 切换为 Root 权限。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun root(sn: String): Int = runForStatus(sn, "root")

    /**
     * 重启。
     *
     * @param[sn] 设备序列号。
     * @param[option] 附加命令，可以携带 `recovery` 、 `fastboot` 、 `bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    @JvmOverloads
    fun reboot(sn: String, option: String = ""): Int {
        val command = if (option.isBlank()) {
            "reboot"
        } else {
            "reboot $option"
        }

        return runForStatus(sn, command)
    }


    /*
     * ----- 设备信息 -----
     */

    /**
     * 获取设备属性。
     *
     * @param[sn] 设备序列号。
     * @param[key] 属性键名。
     * @return 属性值。若属性不存在则返回空值。
     */
    @JvmStatic
    fun getProperty(sn: String, key: String): String? {
        return runForText(sn, "getprop $key")?.trim()
    }

    /**
     * 获取设备属性。
     *
     * @param[sn] 设备序列号。
     * @param[key] 属性键名。
     * @param[default] 默认值。
     * @return 属性值。若属性不存在则返回默认值。
     */
    @JvmStatic
    fun getProperty(sn: String, key: String, default: String): String {
        return runForText(sn, "getprop $key") ?: default
    }

    /**
     * 获取设备制造商。
     *
     * @param[sn] 设备序列号。
     * @return 制造商。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getManufacturer(sn: String): String =
        getProperty(sn, PROPERTY_MANUFACTURER, "")

    /**
     * 获取设备品牌。
     *
     * @param[sn] 设备序列号。
     * @return 品牌。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getBrand(sn: String): String =
        getProperty(sn, PROPERTY_BRAND, "")

    /**
     * 获取设备型号。
     *
     * @param[sn] 设备序列号。
     * @return 型号。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getModel(sn: String): String =
        getProperty(sn, PROPERTY_MODEL, "")

    /**
     * 获取设备 Android 系统版本。
     *
     * @param[sn] 设备序列号。
     * @return 系统版本。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getOSVersion(sn: String): String =
        getProperty(sn, PROPERTY_OSVERSION, "")

    /**
     * 获取设备 Android API Level 。
     *
     * @param[sn] 设备序列号。
     * @return Android API Level 。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getAPILevel(sn: String): String =
        getProperty(sn, PROPERTY_APILEVEL, "")


    /*
     * ----- 文件系统 -----
     */

    /**
     * 发送文件。
     *
     * @param[sn] 设备序列号。
     * @param[src] PC上的文件。
     * @param[remote] 设备上的目标路径。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun pushFile(sn: String, src: File, remote: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s push \"%s\" \"%s\"", sn, src.absolutePath, remote)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 文件系统同步。
     *
     * 有些设备写入文件后需要执行同步命令，否则其他进程读取到的文件数据不完整。
     *
     * @param[sn] 设备序列号。
     */
    @JvmStatic
    fun syncFileSystem(sn: String) {
        runForStatus(sn, "sync")
    }

    /**
     * 重新挂载文件系统。
     *
     * @param[sn] 设备序列号。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    @JvmStatic
    fun remount(sn: String): Boolean {
        val status = runForStatus(sn, "remount")
        return CLIUtil.isSuccess(status)
    }


    /*
     * ----- 网络通信 -----
     */

    /**
     * 开启 TCP/IP 远程控制。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun startRemoteControl(sn: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s tcpip 5555", sn)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 关闭 TCP/IP 远程控制。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun stopRemoteControl(sn: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s usb", sn)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 开启 ADB 端口转发。
     *
     * @param[sn] 设备序列号。
     * @param[pcPort] PC 端口。
     * @param[devicePort] Android 端口。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun startForward(sn: String, pcPort: Int, devicePort: Int): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s forward tcp:%d tcp:%d", sn, pcPort, devicePort)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 关闭 ADB 端口转发。
     *
     * @param[sn] 设备序列号。
     * @param[pcPort] PC 端口。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun stopForward(sn: String, pcPort: Int): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s forward --remove tcp:%d", sn, pcPort)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 开启 ADB 端口转发。
     *
     * @param[sn] 设备序列号。
     * @param[pcEndPoint] PC 端口。
     * @param[deviceEndPoint] Android 端口。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun startForward(sn: String, pcEndPoint: String, deviceEndPoint: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s forward %s %s", sn, pcEndPoint, deviceEndPoint)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 关闭 ADB 端口转发。
     *
     * @param[sn] 设备序列号。
     * @param[pcEndPoint] PC 端口。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun stopForward(sn: String, pcEndPoint: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s forward --remove %s", sn, pcEndPoint)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 清除所有 ADB 端口转发。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    @JvmStatic
    fun cleanForward(sn: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s forward --remove-all", sn)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }


    /*
     * ----- 工具方法 -----
     */

    /**
     * 构建 ADB 命令。
     *
     * 使用当前序列号构建 ADB 命令。若原命令包含 `adb shell` 或 `adb exec-out` 前缀，将会保留；若未指明则使用 `adb shell` 执行命令。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 命令语句。如果输入语句为空，则返回空命令 `:` 。
     */
    @JvmStatic
    fun buildCommand(sn: String, command: String): String {
        var execOut = false
        val matchResult = adbCMDRegex.find(command)

        // 如果命令原有 `adb shell` 等前缀，则先将其移除。
        var trimmed = if (matchResult != null) {
            execOut = matchResult.groupValues[1] == "exec-out"
            command.removeRange(matchResult.range)
                .trim()
        } else {
            command.trim()
        }

        // 如果命令被引号包围，则将其去除，用于空命令判断。
        var trimmedNoQuotes = trimmed
        if (trimmed.length >= 2) {
            val first = trimmed.first()
            val last = trimmed.last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                trimmedNoQuotes = trimmed.substring(1, trimmed.length - 1)
            }
        }

        // 将空指令替换为立刻返回的空语句，防止进入ADB交互式环境导致调用线程被无限阻塞。
        if (trimmedNoQuotes.isBlank()) {
            trimmed = ":"
        }

        // 添加当前序列号并返回原命令内容
        return if (execOut) {
            String.format("${getExecutableName()} -s %s exec-out %s", sn, trimmed)
        } else {
            String.format("${getExecutableName()} -s %s shell %s", sn, trimmed)
        }
    }

    /**
     * 判断语句是否为 ADB 命令。
     *
     * @param[command] 命令语句。
     * @return `true` 表示 ADB 命令， `false` 表示非 ADB 命令。
     */
    @JvmStatic
    fun isADBCommand(command: String): Boolean {
        return command.startsWith("adb ")
    }
}
