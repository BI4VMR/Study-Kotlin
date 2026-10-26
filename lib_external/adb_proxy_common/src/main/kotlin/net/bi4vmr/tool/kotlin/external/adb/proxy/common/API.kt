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

    /**
     * 配对第一步：客户端提交身份与随机数承诺，服务端返回会话 ID 与服务端随机数。
     *
     * 无需认证，请求体为 `PairRequest` ，响应体为 `PairInitResponse` 。
     */
    const val PAIR_INIT: String = "/pair/init"

    /**
     * 配对第二步：客户端公开随机数，服务端展示配对码并等待用户批准，批准后返回令牌。
     *
     * 无需认证，请求体为 `PairRevealRequest` ，响应体为 `PairResponse` 。
     */
    const val PAIR_REVEAL: String = "/pair/reveal"

    /**
     * 配对状态。
     *
     * 需要认证，返回 200 表示当前凭据有效。
     */
    const val PAIR_STATUS: String = "/pair/status"
}
