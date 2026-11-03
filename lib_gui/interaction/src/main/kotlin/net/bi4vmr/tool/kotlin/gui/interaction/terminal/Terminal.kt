package net.bi4vmr.tool.kotlin.gui.interaction.terminal

import net.bi4vmr.tool.java.common.base.system.OSType

/**
 * 终端程序公共能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface Terminal {

    /**
     * 可执行文件名称。
     */
    val executableName: String

    /**
     * 首选操作系统。
     *
     * 部分终端仅支持特定的操作系统，部分终端则是跨平台的。
     *
     * 自动探测可用终端时，优先查找与当前操作系统匹配的终端，未找到再查找其他终端，提高效率。
     */
    val preferOS: OSType
        get() = OSType.UNKNOWN


    /**
     * 回调方法：组装启动命令。
     *
     * 根据当前条件组合参数，返回完整的命令字符串。
     *
     * @param[command] 初始命令。
     * @param[newWindow] 是否在新窗口中打开。
     * @param[keep] 初始命令执行完毕是否保持窗口。
     * @return 启动命令。
     */
    fun onLaunch(command: String?, newWindow: Boolean, keep: Boolean): String
}
