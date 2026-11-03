package net.bi4vmr.tool.kotlin.gui.interaction.terminal

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.system.SystemUtil
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * 终端工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object TerminalUtil {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(TerminalUtil::class.java)

    /**
     * 默认终端程序。
     *
     * 初始化时将会自动侦测一次，若自动侦测失败，则为空值。
     */
    @Volatile
    var defaultTerminal: Terminal?


    init {
        detectDefaultTerminal().let {
            logger.info("Detected default terminal is [$it].")
            defaultTerminal = it
        }
    }


    /**
     * 打开终端程序。
     *
     * @param[command] 初始命令。默认值为空，表示仅打开终端而不执行任何命令。
     * @param[terminal] 终端程序，可以传入 [Terminals] 内置枚举，也可以传入 [Terminal] 接口自定义实现。默认值为空，此时使用默认终端。
     * @param[newWindow] 是否在新窗口中打开。默认值为是。部分终端程序不支持标签页，即使该参数设为否也会在新窗口中打开。
     * @param[keep] 初始命令执行完毕是否保持窗口。本工具用于打开 GUI 终端模拟器，通常都应当设为 `true` 以便用户查看结果；如果只需要执行
     * 命令并在程序内部获取结果，应当使用无界面的 [CLIUtil.run] 系列方法。
     * @return 终端程序进程。
     * @throws[IllegalStateException] 自动侦测失败，没有可用的终端程序。
     * @throws[IOException] 指定的终端程序不存在。
     */
    @JvmStatic
    @JvmOverloads
    fun launch(
        command: String? = null,
        terminal: Terminal? = null,
        newWindow: Boolean = true,
        keep: Boolean = true
    ): Process {
        val target = terminal ?: defaultTerminal
        ?: throw IllegalStateException("No terminal program found! you can set a custom terminal by `defaultTerminal`.")

        val cmds = target.onLaunch(command, newWindow, keep)
        logger.debug("Launch terminal. CMD:[{}]", cmds)
        return CLIUtil.run(cmds)
    }


    /**
     * 自动侦测默认终端程序。
     *
     * @return 默认终端程序。若未找到可用终端则返回空值。
     */
    private fun detectDefaultTerminal(): Terminal? {
        val os = SystemUtil.getOSType()
        // 优先查找与当前操作系统匹配的终端
        Terminals.entries
            .filter { it.preferOS == os }
            .forEach {
                if (CLIUtil.hasExecutable(it.executableName)) {
                    return it
                }
            }

        // 如果未找到当前平台的可用终端，再尝试查找是否有跨平台终端实现。
        Terminals.entries
            .filter { it.preferOS != os }
            .forEach {
                if (CLIUtil.hasExecutable(it.executableName)) {
                    return it
                }
            }

        return null
    }
}
