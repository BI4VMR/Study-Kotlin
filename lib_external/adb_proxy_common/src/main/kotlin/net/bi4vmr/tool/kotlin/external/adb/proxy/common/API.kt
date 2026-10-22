package net.bi4vmr.tool.kotlin.external.adb.proxy.common

import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBCastStartRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBDeviceRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBExecRequest

/**
 * URL 定义。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object API {

    /**
     * 查询通信协议版本。
     *
     * 请求方式为 HTTP GET ，返回纯文本，内容为版本号。
     */
    const val VERSION: String = "/version"

    /**
     * 查询设备列表。
     *
     * 请求方式为 HTTP POST ，返回 JSON 设备列表。
     */
    const val ADB_DEVICES: String = "/adb/devices"

    /**
     * 执行 ADB Shell 命令并返回状态码。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBExecRequest] ；返回纯文本，内容为命令的状态码。
     */
    const val ADB_EXEC_STATUS: String = "/adb/exec/status"

    /**
     * 执行 ADB Shell 命令并返回文本。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBExecRequest] ；返回纯文本，内容为命令的输出文本。
     */
    const val ADB_EXEC_TEXT: String = "/adb/exec/text"

    /**
     * 查询设备屏幕信息。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBDeviceRequest] ；返回纯文本，内容为命令的输出文本。
     */
    const val ADB_DISPLAYS: String = "/adb/displays"

    /**
     * 请求远程设备开启投屏转发。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBCastStartRequest] ；返回纯文本，内容为服务端音视频数据的转发端口，请求者需要使用
     * `ADBCastReceiver` 建立连接并进行解码。
     */
    const val ADB_CAST_START: String = "/adb/cast/start"

    /**
     * 信令消息。
     *
     * 服务端用于通告设备状态变更等事件。
     *
     * 协议为 WebSocket 。
     */
    const val ADB_EVENTS: String = "/adb/events"

    /**
     * 远程 ADB Shell 。
     *
     * 协议为 WebSocket ，请求体为 [ADBDeviceRequest] 。
     */
    const val ADB_SHELL: String = "/adb/shell"
}
