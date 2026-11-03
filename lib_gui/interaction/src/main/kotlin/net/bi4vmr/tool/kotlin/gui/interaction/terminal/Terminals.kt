@file:OptIn(ExperimentalUuidApi::class)

package net.bi4vmr.tool.kotlin.gui.interaction.terminal

import net.bi4vmr.tool.java.common.base.system.OSType
import net.bi4vmr.tool.java.common.base.system.SystemUtil
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 知名终端程序。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class Terminals : Terminal {

    /**
     * Konsole.
     *
     * 若要使新标签页在已有进程中打开，必须在 Konsole 设置中勾选“在同一个进程内运行所有窗口”，否则标签页仍会在新窗口中启动。
     */
    KONSOLE {

        override val executableName: String = "konsole"

        override val preferOS: OSType = OSType.LINUX

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
                .append(executableName)
                .append(" ")
                .apply {
                    if (!newWindow) append("--new-tab ")

                    // 如果未指定初始命令，则使用空命令 `:` 。
                    val cmd = (command ?: ":")
                    val userShell = SystemUtil.getShell().ifBlank { "/bin/sh" }

                    /*
                     * `-e` 表示初始命令，必须在其他选项之后。
                     *
                     * `$SHELL -ic` 表示读取当前用户配置文件后再执行命令，否则部分环境变量未加载会导致找不到命令。
                     */
                    append("-e $userShell -ic \"$cmd")

                    if (keep) {
                        // `exec $SHELL` 表示前一条命令执行完毕后进入交互式 Shell，保持窗口不退出。
                        append("; exec $userShell;\"")
                    } else {
                        // 前一条语句末尾未添加双引号，此处需要补齐。
                        append("\"")
                    }
                }

            return cmd.toString()
        }
    },

    /**
     * GNOME Terminal.
     */
    GNOME_TERMINAL {

        override val executableName: String = "gnome-terminal"

        override val preferOS: OSType = OSType.LINUX

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
                .append(executableName)
                .append(" ")
                .apply {
                    if (newWindow) append("--window") else append("--tab")

                    /*
                     * 如果未指定初始命令，则使用空命令 `:` 。
                     *
                     * 如果原命令中有特殊符号，则需要添加转义符号，否则无法被正确解析。
                     */
                    val cmd = (command ?: ":")
                    val userShell = SystemUtil.getShell().ifBlank { "/bin/sh" }

                    /*
                     * `--` 表示初始命令，必须在其他选项之后。
                     *
                     * `$SHELL -ic` 表示读取当前用户配置文件后再执行命令，否则部分环境变量未加载会导致找不到命令。
                     */
                    append("-e $userShell -ic \"$cmd")

                    if (keep) {
                        // `exec $SHELL` 表示前一条命令执行完毕后进入交互式 Shell，保持窗口不退出。
                        append("; exec $userShell;\"")
                    } else {
                        // 前一条语句末尾未添加双引号，此处需要补齐。
                        append("\"")
                    }
                }

            return cmd.toString()
        }
    },

    /**
     * XTerm.
     */
    XTERM {

        override val executableName: String = "xterm"

        override val preferOS: OSType = OSType.LINUX

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
                .append(executableName)
                .append(" ")
                .apply {
                    // 本软件没有标签功能，忽略 `newWindow` 参数。

                    /*
                     * 如果未指定初始命令，则使用空命令 `:` 。
                     *
                     * 如果原命令中有特殊符号，则需要添加转义符号，否则无法被正确解析。
                     */
                    val cmd = (command ?: ":")
                    val userShell = SystemUtil.getShell().ifBlank { "/bin/sh" }

                    /*
                     * `-e` 表示初始命令，必须在其他选项之后。
                     *
                     * `$SHELL -ic` 表示读取当前用户配置文件后再执行命令，否则部分环境变量未加载会导致找不到命令。
                     */
                    append("-e $userShell -ic \"$cmd")

                    if (keep) {
                        // `exec $SHELL` 表示前一条命令执行完毕后进入交互式 Shell，保持窗口不退出。
                        append("; exec $userShell;\"")
                    } else {
                        // 前一条语句末尾未添加双引号，此处需要补齐。
                        append("\"")
                    }
                }

            return cmd.toString()
        }
    },

    /**
     * MacOS Terminal.
     */
    MACOS_TERMINAL {

        override val executableName: String = "osascript"

        override val preferOS: OSType = OSType.MACOS

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
            // TODO 暂未实现
            return cmd.toString()
        }
    },

    /**
     * Windows Terminal.
     */
    WINDOWS_TERMINAL {

        override val executableName: String = "wt"

        override val preferOS: OSType = OSType.WINDOWS

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
                .append(executableName)
                .apply {
                    // `-w <窗口名称>` 表示在指定窗口打开，使用随机 ID 确保每次执行都创建新窗口。 `-w 0` 表示复用最近使用的窗口。
                    if (newWindow) append(" -w ${Uuid.generateV4()}") else append(" -w 0")

                    if (command != null) {
                        // Windows Terminal 无法直接执行命令，此处使用 `cmd` 桥接执行目标命令。
                        if (keep) append(" cmd /k $command") else append(" cmd /c $command")
                    }
                }

            return cmd.toString()
        }
    },

    /**
     * Windows PowerShell.
     */
    POWER_SHELL {

        override val executableName: String = "powershell"

        override val preferOS: OSType = OSType.WINDOWS

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
                /*
                 * 在 Windows 10 及以上版本，若安装了 Windows Terminal 再执行 `powershell` 命令，会启动 Windows Terminal ，指明
                 * `conhost` 则会启动传统控制台窗口。
                 */
                .append("conhost")
                .append(" ")
                .append(executableName)
                .apply {
                    // 本软件没有标签功能，忽略 `newWindow` 参数。

                    if (keep) append(" -NoExit")

                    if (command != null) {
                        append(" -Command $command")
                    }
                }

            return cmd.toString()
        }
    },

    /**
     * Windows 命令提示符。
     */
    CMD {

        override val executableName: String = "cmd"

        override val preferOS: OSType = OSType.WINDOWS

        override fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String {
            val cmd = StringBuilder()
                /*
                 * 在 Windows 10 及以上版本，若安装了 Windows Terminal 再执行 `cmd` 命令，会启动 Windows Terminal ，指明 `conhost`
                 * 则会启动传统控制台窗口。
                 */
                .append("conhost")
                .append(" ")
                .append(executableName)
                .apply {
                    // 本软件没有标签功能，忽略 `newWindow` 参数。

                    if (command != null) {
                        append(" ")
                        // `/c` 表示命令执行完毕后终端自动关闭； `/k` 表示窗口保持打开。
                        if (keep) append("/k $command") else append("/c $command")
                    }
                }

            return cmd.toString()
        }
    };
}
