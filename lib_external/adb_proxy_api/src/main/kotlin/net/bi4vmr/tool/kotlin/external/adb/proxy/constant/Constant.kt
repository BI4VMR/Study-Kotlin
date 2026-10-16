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
}
