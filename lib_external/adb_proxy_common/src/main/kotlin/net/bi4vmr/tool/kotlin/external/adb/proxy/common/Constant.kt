package net.bi4vmr.tool.kotlin.external.adb.proxy.common

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant.PARAM_SN
import net.bi4vmr.tool.kotlin.external.adb.proxy.http.ADBExecRequest

/**
 * 全局常量。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Constant {

    /**
     * 服务端默认端口。
     */
    const val PORT_SERVER_DEFAULT: Int = 5038

    /**
     * 通信协议版本。
     *
     * 出现破坏性变更时应增加版本号，客户端建立连接后应检查双方版本号是否兼容。
     */
    const val PROTOCOL_VERSION: String = "1"

    /**
     * 路径：查询通信协议版本。
     *
     * 请求格式为 HTTP GET ，返回版本号字符串。
     */
    const val PATH_VERSION: String = "/version"

    /**
     * 路径：查询设备列表。
     *
     * 请求格式为 HTTP POST ，返回 JSON 。
     */
    const val PATH_ADB_DEVICE_LIST: String = "/adb/devices"

    /**
     * 路径：执行 ADB Shell 命令并返回状态码。
     *
     * 请求格式为 HTTP POST ，请求体为 [ADBExecRequest] ；返回纯文本消息，内容为命令的状态码。
     */
    const val PATH_ADB_EXEC_STATUS: String = "/adb/exec/status"

    /**
     * 路径：执行 ADB Shell 命令并返回文本。
     *
     * 请求格式为 HTTP POST ，请求体为 [ADBExecRequest] ；返回纯文本消息，内容为命令的输出文本。
     */
    const val PATH_ADB_EXEC_TEXT: String = "/adb/exec/text"

    const val PATH_ADB_DISPLAYS: String = "/adb/displays"

    /**
     * 路径：信令消息（服务端用于通告设备状态变更等事件）。
     *
     * 协议为 WebSocket 。
     */
    const val PATH_ADB_EVENTS: String = "/adb/events"

    /**
     * 路径：远程 ADB Shell 。
     *
     * 协议为 WebSocket ，查询参数 [PARAM_SN] 表示目标设备序列号。
     */
    const val PATH_ADB_SHELL: String = "/adb/shell"

    /**
     * 查询参数： ADB 设备序列号。
     */
    const val PARAM_SN: String = "sn"
}
