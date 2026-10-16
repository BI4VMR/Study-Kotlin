package net.bi4vmr.tool.kotlin.external.adb.proxy.constant

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
     * 路径：事件推送（设备状态变更等）。
     *
     * 协议为 WebSocket 。
     */
    const val PATH_ADB_EVENTS: String = "/adb/events"

    /**
     * 路径：交互式 Shell 。
     *
     * 协议为 WebSocket ，查询参数 [PARAM_SN] 指定目标设备序列号。二进制帧为终端原始字节流，双向透传，连接关闭即结束会话。
     */
    const val PATH_ADB_SHELL: String = "/adb/shell"

    /**
     * 查询参数：设备序列号。
     */
    const val PARAM_SN: String = "sn"

    /**
     * 查询参数：终端类型。
     *
     * 对应客户端终端的 `TERM` 环境变量，决定设备 Shell 是否启用方向键、Tab 补全等行编辑功能，缺省值为 `xterm-256color` 。
     */
    const val PARAM_TERM: String = "term"

    /**
     * 查询参数：终端列数。
     */
    const val PARAM_COLS: String = "cols"

    /**
     * 查询参数：终端行数。
     */
    const val PARAM_ROWS: String = "rows"
}
