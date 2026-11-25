package net.bi4vmr.tool.kotlin.external.adb.common.util

import net.bi4vmr.tool.java.common.base.io.BaseIOUtil
import net.bi4vmr.tool.java.common.base.io.FileIOUtil
import net.bi4vmr.tool.java.common.base.io.IOUtil
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.constants.Intents
import net.bi4vmr.tool.kotlin.external.adb.common.constants.Propertys
import net.bi4vmr.tool.kotlin.external.adb.common.enums.AndroidVersion
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper.getExecutableName
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminal
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.TerminalUtil
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminals
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

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
     * 正则表达式：ADB 命令。
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
     * @param[redirectError] 是否重定向错误输出流。默认值为 `true`。
     * @return [Process] 实例，永不为空，进程启动失败时抛出异常。
     * @throws[IOException] 进程启动失败。
     */
    @JvmStatic
    @JvmOverloads
    fun run(sn: String, command: String, redirectError: Boolean = true): Process {
        val optimizedCommand = buildCommandArgs(sn, command)
        return CLIUtil.run(optimizedCommand, redirectError)
    }

    /**
     * 执行 ADB Shell 命令并获取返回值。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmStatic
    fun runForStatus(sn: String, command: String): Int {
        val optimizedCommand = buildCommandArgs(sn, command)
        return CLIUtil.runForStatus(optimizedCommand)
    }

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true`。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun runForLines(
        sn: String,
        command: String,
        redirectError: Boolean = false
    ): List<String>? {
        val optimizedCommand = buildCommandArgs(sn, command)
        return CLIUtil.runForLines(optimizedCommand, redirectError, StandardCharsets.UTF_8.name())
    }

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true`。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun runForText(sn: String, command: String, redirectError: Boolean = false): String? {
        val optimizedCommand = buildCommandArgs(sn, command)
        return CLIUtil.runForText(optimizedCommand, redirectError, StandardCharsets.UTF_8.name())
    }

    /**
     * 执行 ADB Shell 命令并获取错误消息。
     * <p>
     * 执行命令并阻塞当前线程，读取错误输出并转为文本。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 命令的错误输出文本。若出现异常则返回空值。
     */
    @JvmStatic
    fun runForError(sn: String, command: String): String? {
        val optimizedCommand = buildCommandArgs(sn, command)
        return CLIUtil.runForError(optimizedCommand, StandardCharsets.UTF_8.name())
    }

    /**
     * 打开交互式 Shell。
     *
     * @param[sn] 设备序列号。
     * @param[terminal] 终端程序，可以传入 [Terminals] 内置枚举，也可以传入 [Terminal] 接口自定义实现。默认值为空，此时使用默认终端。
     * @param[newWindow] 是否在新窗口中打开。默认值为是。部分终端程序不支持标签页，即使该参数设为否也会在新窗口中打开。
     * @param[keep] 初始命令执行完毕是否保持窗口。默认值为是。
     * @return 终端程序进程。
     * @throws[IllegalStateException] 没有可用的终端程序。
     */
    @JvmStatic
    @JvmOverloads
    fun openShell(
        sn: String,
        terminal: Terminal? = null,
        newWindow: Boolean = true,
        keep: Boolean = true
    ): Process =
        TerminalUtil.launch(
            terminal = terminal,
            command = "${getExecutableName()} -s $sn shell",
            newWindow = newWindow,
            keep = keep
        )

    /**
     * 切换为 Root 权限。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmStatic
    fun root(sn: String): Int =
        runForStatus(sn, "root")

    /**
     * 重启。
     *
     * @param[sn] 设备序列号。
     * @param[option] 附加命令，可以携带 `recovery`、`fastboot`、`bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmStatic
    @JvmOverloads
    fun reboot(sn: String, option: String = ""): Int =
        runForStatus(sn, "reboot $option".trim())


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
    fun getProperty(sn: String, key: String): String? =
        runForText(sn, "getprop $key")?.trim()

    /**
     * 获取设备属性。
     *
     * @param[sn] 设备序列号。
     * @param[key] 属性键名。
     * @param[default] 默认值。
     * @return 属性值。若属性不存在则返回默认值。
     */
    @JvmStatic
    fun getProperty(sn: String, key: String, default: String): String =
        runForText(sn, "getprop $key") ?: default

    /**
     * 获取设备制造商。
     *
     * @param[sn] 设备序列号。
     * @return 制造商。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getManufacturer(sn: String): String =
        getProperty(sn, Propertys.Key.MANUFACTURER, "")

    /**
     * 获取设备品牌。
     *
     * @param[sn] 设备序列号。
     * @return 品牌。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getBrand(sn: String): String =
        getProperty(sn, Propertys.Key.BRAND, "")

    /**
     * 获取设备型号。
     *
     * @param[sn] 设备序列号。
     * @return 型号。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getModel(sn: String): String =
        getProperty(sn, Propertys.Key.MODEL, "")

    /**
     * 获取设备 Android 系统版本。
     *
     * @param[sn] 设备序列号。
     * @return 系统版本。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getOSVersion(sn: String): String =
        getProperty(sn, Propertys.Key.OS_VERSION, "")

    /**
     * 获取设备 Android API Level。
     *
     * @param[sn] 设备序列号。
     * @return Android API Level。若设备未声明则为空字符串。
     */
    @JvmStatic
    fun getAPILevel(sn: String): String =
        getProperty(sn, Propertys.Key.API_LEVEL, "")


    /*
     * ----- 图形界面 -----
     */

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
     * @param[displayID] 显示屏 ID。
     * @return 索引序号。无法获取时返回 `0`。
     */
    @JvmStatic
    fun getDisplayUniqueIndex(sn: String, displayID: String): String {
        // 对于 Android 9 及更低版本可能解析不到数据，此时默认返回 `0`。
        return getDisplays(sn)
            .find { it.id == displayID }
            ?.uniqueID
            ?: "0"
    }

    /**
     * 获取显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回空值。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return 分辨率信息。若显示屏不存在则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun getDisplaySize(sn: String, displayID: String = "0"): DisplaySize? {
        val version = AndroidVersion.fromAPILevel(getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return null
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm size"
            }
        } else {
            "wm size -d $displayID"
        }

        val lines = runForLines(sn, cmd) ?: return null
        return ADBOutputParser.parseWMSize(lines)
    }

    /**
     * 设置显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[width] 宽度。
     * @param[height] 高度。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    @JvmOverloads
    fun setDisplaySize(sn: String, width: Int, height: Int, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm size ${width}x$height"
            }
        } else {
            "wm size ${width}x$height -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 还原显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    @JvmOverloads
    fun resetDisplaySize(sn: String, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm size reset"
            }
        } else {
            "wm size reset -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 获取显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回空值。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return 密度信息。若出现异常则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun getDisplayDensity(sn: String, displayID: String = "0"): DisplayDensity? {
        val version = AndroidVersion.fromAPILevel(getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return null
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm density"
            }
        } else {
            "wm density -d $displayID"
        }

        val lines = runForLines(sn, cmd) ?: return null
        return ADBOutputParser.parseWMDensity(lines)
    }

    /**
     * 设置显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[density] 密度。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    @JvmOverloads
    fun setDisplayDensity(sn: String, density: Int, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm density $density"
            }
        } else {
            "wm density $density -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 还原显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    @JvmOverloads
    fun resetDisplayDensity(sn: String, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm density reset"
            }
        } else {
            "wm density reset -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 获取 Activity 列表。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认为空值，表示列出所有显示屏的 Activity。
     * @return Activity 列表。命令执行失败时返回空列表。每个显示屏内按照从顶层到底层的顺序排列。
     */
    @JvmStatic
    @JvmOverloads
    fun getActivities(sn: String, displayID: String? = null): List<ActivityInfo> {
        val lines = runForLines(sn, "dumpsys activity activities") ?: emptyList()
        val list = ADBOutputParser.parseDumpActivities(lines)
        return if (displayID == null) list else list.filter { it.displayID == displayID }
    }

    /**
     * 获取 Window 列表。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认为空值，表示列出所有显示屏的 Window。
     * @return Window 列表。命令执行失败时返回空列表。每个显示屏内按照从顶层到底层的顺序排列。
     */
    @JvmStatic
    @JvmOverloads
    fun getWindows(sn: String, displayID: String? = null): List<WindowInfo> {
        val lines = runForLines(sn, "dumpsys window windows") ?: emptyList()
        val list = ADBOutputParser.parseDumpWindows(lines)
        return if (displayID == null) list else list.filter { it.displayID == displayID }
    }

    /**
     * 获取全局焦点信息。
     *
     * Android 9 及更低版本多屏 API 不完善，焦点所在屏幕始终为 `0`。
     *
     * @param[sn] 设备序列号。
     * @return 全局焦点信息。
     */
    @JvmStatic
    fun getGlobalFocusInfo(sn: String): FocusInfo {
        val cmd = """
                dumpsys window | grep -iE "Display: mDisplayId=|mCurrentFocus=|mFocusedApp=|FocusedWindow=|mTopFocusedDisplayId="
                """.trimIndent()
        val lines = runForLines(sn, cmd) ?: return FocusInfo()
        val result = ADBOutputParser.parseGlobalFocusInfo(lines)

        // Android 9 及更低版本的 Activity 信息没有焦点信息，回退到通过 Window 信息获取。
        if (result.windowHash != "-") return result
        val linesFallback = runForLines(sn, "dumpsys window windows") ?: return result
        return ADBOutputParser.parseGlobalFocusInfoV9L(linesFallback)
    }

    /**
     * 获取屏幕截图。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID。
     * @return 二进制数据。若出现错误则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun screenshotAsPNG(sn: String, displayID: String? = null): ByteArray? {
        try {
            val cmd = if (displayID == null) {
                String.format("${getExecutableName()} -s %s exec-out screencap -p", sn)
            } else {
                val physicalID = getDisplayUniqueIndex(sn, displayID)
                String.format("${getExecutableName()} -s %s exec-out screencap -d %s -p", sn, physicalID)
            }

            val process = CLIUtil.run(cmd, false)
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
     * @param[displayID] 显示屏 ID。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    @JvmOverloads
    fun screenshotAsPNG(sn: String, file: File, displayID: String? = null): Boolean {
        try {
            val cmd = if (displayID == null) {
                String.format("${getExecutableName()} -s %s exec-out screencap -p", sn)
            } else {
                val physicalID = getDisplayUniqueIndex(sn, displayID)
                String.format("${getExecutableName()} -s %s exec-out screencap -d %s -p", sn, physicalID)
            }

            val process = CLIUtil.run(cmd, false)
            // 丢弃错误输出，防止阻塞其他线程。
            BaseIOUtil.readAndDrop(process.errorStream)
            return FileIOUtil.saveToFile(process.inputStream, file, IOUtil.BUFFER_SIZE_8MB)
        } catch (_: Exception) {
            return false
        }
    }


    /*
     * ----- 文件系统 -----
     */

    /**
     * 推送文件到 Android 设备。
     *
     * `adb push` 命令封装，支持目录，适用于无需获取进度的简单场景。
     *
     * @param[sn] 设备序列号。
     * @param[local] 源路径（PC 端）。
     * @param[remote] 目标路径（Android 端）。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmStatic
    fun pushFile(sn: String, local: File, remote: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s push \"%s\" \"%s\"", sn, local.absolutePath, remote)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 从 Android 设备拉取文件。
     *
     * `adb pull` 命令封装，支持目录，适用于无需获取进度的简单场景。
     *
     * @param[sn] 设备序列号。
     * @param[remote] 源路径（Android 端）。
     * @param[local] 目标路径（PC 端）。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmStatic
    fun pullFile(sn: String, remote: String, local: File): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s pull \"%s\" \"%s\"", sn, remote, local.absolutePath)
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
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    fun remount(sn: String): Boolean {
        val status = runForStatus(sn, "remount")
        return CLIUtil.isSuccess(status)
    }

    /**
     * 触发媒体扫描。
     *
     * @param[sn] 设备序列号。
     * @param[path] 扫描路径。默认值为根目录 `/`。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmStatic
    @JvmOverloads
    fun mediaScan(sn: String, path: String = "/"): Boolean {
        val cmd = "am broadcast -a ${Intents.Action.MEDIA_SCAN} -d file:/$path"
        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }


    /*
     * ----- 网络通信 -----
     */

    /**
     * 开启 TCP/IP 远程控制。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1`。
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
     * @return 状态码。若出现异常则返回 `-1`。
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
     * @return 状态码。若出现异常则返回 `-1`。
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
     * @return 状态码。若出现异常则返回 `-1`。
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
     * @return 状态码。若出现异常则返回 `-1`。
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
     * @return 状态码。若出现异常则返回 `-1`。
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
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmStatic
    fun cleanForward(sn: String): Boolean {
        val cmd = String.format("${getExecutableName()} -s %s forward --remove-all", sn)
        val status = CLIUtil.runForStatus(cmd)
        return CLIUtil.isSuccess(status)
    }


    /*
     * ----- 软件包管理 -----
     */

    /**
     * 获取软件包列表。
     *
     * @param[sn] 设备序列号。
     * @param[type] 类型筛选条件。默认值为 [PackageType.ALL]，不设筛选条件。
     * @return 包名列表。永不为空值，命令执行异常时返回空列表。
     */
    @JvmOverloads
    fun listPackages(sn: String, type: PackageType = PackageType.ALL): List<String> {
        val lines = runForLines(sn, "pm list package ${type.cmdArgs}") ?: emptyList()
        return ADBOutputParser.parsePMListPackageOutput(lines)
    }

    /**
     * 安装 PC 端软件包。
     *
     * @param[sn] 设备序列号。
     * @param[file] APK 文件。
     * @throws[Exception] 安装失败。消息为 ADB 命令输出的错误信息。
     */
    fun installPackage(sn: String, file: File) {
        val cmd = String.format("${getExecutableName()} -s %s install -r -t \"%s\"", sn, file.canonicalPath)
        val process = CLIUtil.run(cmd, false)
        BaseIOUtil.readAndDrop(process.inputStream)
        val errorInfo = BaseIOUtil.readAsText(process.errorStream)
        if (errorInfo.isNotBlank()) {
            throw Exception(errorInfo.removePrefix("adb: "))
        }

        CLIUtil.stopProcess(process)
    }

    /**
     * 卸载软件包。
     *
     * @param[sn] 设备序列号。
     * @param[packageName] 包名。
     * @param[userID] 用户 ID。对于预装应用等组件无法直接卸载，可以尝试在特定用户空间将其标记为伪卸载。默认为空值，表示全局卸载。
     * @param[keepData] 是否保留数据。默认不保留数据。
     * @throws[Exception] 卸载失败。消息为 ADB 命令输出的错误信息。
     */
    @JvmStatic
    @JvmOverloads
    fun uninstallPackage(sn: String, packageName: String, userID: String? = null, keepData: Boolean = false) {
        val builder = StringBuilder(getExecutableName())
            .append(" -s $sn")
            .append(" uninstall")
        if (keepData) {
            builder.append(" -k")
        }
        if (userID != null) {
            builder.append(" --user $userID")
        }
        builder.append(" \'$packageName\'")

        val error = runForError(sn, builder.toString()) ?: ""
        if (error.isNotBlank()) {
            throw Exception(error)
        }
    }

    /**
     * 启用软件包。
     *
     * @param[sn] 设备序列号。
     * @param[packageName] 包名。
     * @param[userID] 用户 ID。默认为空值，表示不指定用户。
     * @throws[Exception] 启用失败。消息为 `pm` 命令输出的错误信息。
     */
    @JvmStatic
    @JvmOverloads
    fun enablePackage(sn: String, packageName: String, userID: String? = null) {
        val builder = StringBuilder("pm enable")
        if (userID != null) {
            builder.append(" --user $userID")
        }
        builder.append(" \'$packageName\'")

        val error = runForError(sn, builder.toString()) ?: ""
        if (error.isNotBlank()) {
            throw Exception(error)
        }
    }

    /**
     * 禁用软件包。
     *
     * 优先使用 `pm disable` 命令；若执行失败则回退至 `pm disable-user` 命令。
     *
     * @param[sn] 设备序列号。
     * @param[packageName] 包名。
     * @param[userID] 用户 ID。默认为空值，表示不指定用户。
     * @throws[Exception] 禁用失败。消息为 `pm` 命令输出的错误信息。
     */
    @JvmStatic
    @JvmOverloads
    fun disablePackage(sn: String, packageName: String, userID: String? = null) {
        try {
            val builder = StringBuilder("pm disable")
            if (userID != null) {
                builder.append(" --user $userID")
            }
            builder.append(" \'$packageName\'")

            val error = runForError(sn, builder.toString()) ?: ""
            if (error.isNotBlank()) {
                throw Exception(error)
            }
        } catch (_: Exception) {
            // `disable-user` 是较新版本提供的命令，对于多用户系统如果 `disable` 命令执行失败，则尝试使用该命令。
            val builder = StringBuilder("pm disable-user")
            if (userID != null) {
                builder.append(" --user $userID")
            }
            builder.append(" \'$packageName\'")

            val error = runForError(sn, builder.toString()) ?: ""
            if (error.isNotBlank()) {
                throw Exception(error)
            }
        }
    }


    /*
     * ----- 工具方法 -----
     */

    /**
     * 构建 ADB 命令。
     *
     * 使用当前设备构建 ADB 命令。若原命令包含 `adb shell` 或 `adb exec-out` 前缀，将会保留；若未指明前缀默认添加 `adb shell`。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 命令语句。如果输入语句为空，则返回空命令 `:`。
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

        // 将 `adb shell ""` 指令替换为立刻返回的空语句，防止进入 ADB 交互式环境导致调用线程被无限阻塞。
        if (trimmedNoQuotes.isBlank()) {
            trimmed = ":"
        }

        // 添加当前序列号并返回原命令内容
        return if (execOut) {
            String.format("${getExecutableName()} -s %s exec-out \'%s\'", sn, trimmed)
        } else {
            String.format("${getExecutableName()} -s %s shell \'%s\'", sn, trimmed)
        }
    }


    /**
     * 构建 ADB 命令数组。
     *
     * [buildCommand] 方法生成的命令行便于人类阅读，但不适合创建进程，本方法将生成可用于远程执行的参数数组。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句。
     * @return 命令语句。如果输入语句为空，则返回空命令 `:`。
     */
    @JvmStatic
    fun buildCommandArgs(sn: String, command: String): Array<String> {
        val cmd = buildCommand(sn, command)
        // 远程命令起始位置
        val quoteIndex = cmd.indexOf(" \'")
        if (quoteIndex < 0) {
            return emptyArray()
        }

        // `adb -s <sn> shell` 或 `adb -s <sn> exec-out` 前缀
        val prefix = cmd.substring(0, quoteIndex)
        val prefixArgs = CLIUtil.parseCommand(prefix)
        // 单引号之间的命令部分
        val suffix = cmd.substring(quoteIndex + 2, cmd.length - 1)
        // `parseCommand()` 方法将会移除引号并进行解析，无法处理带有空格的文件名等参数，因此仅处理前缀部分，后缀部分作为一个参数传入。
        return arrayOf(*prefixArgs, suffix)
    }

    /**
     * 判断语句是否为 ADB 命令。
     *
     * @param[command] 命令语句。
     * @return `true` 表示 ADB 命令，`false` 表示非 ADB 命令。
     */
    @JvmStatic
    fun isADBCommand(command: String): Boolean {
        return command.startsWith("adb ")
    }

    /**
     * 判断语句是否为 ADB Shell 命令。
     *
     * @param[command] 命令语句。
     * @return `true` 表示 ADB Shell 命令，`false` 表示非 ADB Shell 命令。
     */
    @JvmStatic
    fun isADBShellCommand(command: String): Boolean {
        return adbCMDRegex.matches(command)
    }
}
