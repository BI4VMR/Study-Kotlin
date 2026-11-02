package net.bi4vmr.tool.kotlin.gui.interaction.terminal

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.system.SystemUtil

/**
 * 终端工具。
 *
 * 用于打开系统终端程序，并可在其中执行指定的命令。内置了常见的终端（[BuiltinTerminal]），
 * 也支持通过 [CustomTerminal] 配置任意终端程序。
 *
 * 示例：
 *
 * ```kotlin
 * // 使用默认终端（自动探测或由用户设置）
 * TerminalUtil.open(command = "ls -l", newWindow = true)
 *
 * // 指定内置终端
 * TerminalUtil.open(BuiltinTerminal.KONSOLE, "top")
 *
 * // 自定义终端
 * TerminalUtil.defaultTerminal = CustomTerminal(
 *     executable = "alacritty",
 *     commandArgs = listOf("-e", "bash", "-c", "${CustomTerminal.PH_COMMAND}; exec bash")
 * )
 * TerminalUtil.open(command = "htop")
 * ```
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object TerminalUtil {

    @JvmStatic
    fun open(
        terminal: Terminal? = null,
        command: String? = null,
        newWindow: Boolean = true
    ): Process {
        val name = terminal?.executableName ?: "TODO"
        return CLIUtil.run("$name ${command.orEmpty()}")
    }


    /**
     * 判断当前环境是否存在可执行程序。
     *
     * @param name 程序名称。
     * @return `true` 表示程序存在； `false` 表示程序不存在。
     */
    private fun hasExecutable(name: String): Boolean {
        val detectProgram = if (SystemUtil.isWindows()) "where" else "which"
        val status = CLIUtil.runForStatus("$detectProgram $name")
        return CLIUtil.isSuccess(status)
    }
}


/*
enum class BuiltinTerminal(
    val os: OSType,
    val executable: String,
    val hasOwnWindow: Boolean
) : Terminal {

    /** Windows Terminal */
    WINDOWS_TERMINAL(OSType.WINDOWS, "wt", true) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            // "-w new" 打开新窗口，"-w 0" 在最近使用的窗口中打开新标签页
            val args = mutableListOf(executable, "-w", if (newWindow) "new" else "0", "new-tab")
            if (command != null) {
                args += listOf("cmd", "/k", command)
            }
            return args
        }
    },

    /** PowerShell 7+ */
    PWSH(OSType.WINDOWS, "pwsh", false) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> =
            powerShellArgs(executable, command, newWindow)
    },

    /** Windows PowerShell */
    POWERSHELL(OSType.WINDOWS, "powershell", false) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> =
            powerShellArgs(executable, command, newWindow)
    },

    /** 命令提示符 */
    CMD(OSType.WINDOWS, "cmd", false) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            val inner = mutableListOf(executable)
            if (command != null) {
                inner += listOf(if (newWindow) "/k" else "/c", command)
            }
            return if (newWindow) startInNewWindow(inner) else inner
        }
    },

    /** KDE Konsole */
    KONSOLE(OSType.LINUX, "konsole", true) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            val args = mutableListOf(executable)
            if (!newWindow) args += "--new-tab"
            if (command != null) args += listOf("-e") + shellKeepAlive(command)
            return args
        }
    },

    /** GNOME Terminal */
    GNOME_TERMINAL(OSType.LINUX, "gnome-terminal", true) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            val args = mutableListOf(executable, if (newWindow) "--window" else "--tab")
            if (command != null) args += listOf("--") + shellKeepAlive(command)
            return args
        }
    },

    /** XFCE Terminal */
    XFCE4_TERMINAL(OSType.LINUX, "xfce4-terminal", true) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            val args = mutableListOf(executable, if (newWindow) "--window" else "--tab")
            if (command != null) args += listOf("-x") + shellKeepAlive(command)
            return args
        }
    },

    /** XTerm（不支持标签页，始终打开新窗口） */
    XTERM(OSType.LINUX, "xterm", true) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            val args = mutableListOf(executable)
            if (command != null) args += listOf("-e") + shellKeepAlive(command)
            return args
        }
    },

    /** macOS Terminal.app（通过 AppleScript 控制，始终打开新窗口） */
    MAC_TERMINAL(OSType.MACOS, "osascript", true) {
        override fun buildArgs(command: String?, newWindow: Boolean): List<String> {
            val escaped = command.orEmpty().replace("\\", "\\\\").replace("\"", "\\\"")
            return listOf(
                executable,
                "-e", "tell application \"Terminal\" to do script \"$escaped\"",
                "-e", "tell application \"Terminal\" to activate"
            )
        }
    };

    companion object {

        private fun powerShellArgs(exe: String, command: String?, newWindow: Boolean): List<String> {
            val inner = mutableListOf(exe, "-NoLogo")
            if (command != null) {
                if (newWindow) inner += "-NoExit"
                inner += listOf("-Command", command)
            }
            return if (newWindow) startInNewWindow(inner) else inner
        }

        /** 通过 `cmd /c start` 在新的控制台窗口中运行程序。 */
        private fun startInNewWindow(inner: List<String>): List<String> =
            listOf("cmd", "/c", "start", "\"\"") + inner

        /** 执行命令后保留交互式 Shell ，避免窗口立即关闭。 */
        private fun shellKeepAlive(command: String): List<String> {
            val shell = System.getenv("SHELL")?.takeIf { it.isNotBlank() } ?: "bash"
            return listOf(shell, "-c", "$command; exec $shell")
        }
    }
}
*/
