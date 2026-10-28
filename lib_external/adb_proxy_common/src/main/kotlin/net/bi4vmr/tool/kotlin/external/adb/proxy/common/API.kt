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

    /* ----- 系统信息 ----- */

    /**
     * 查询服务器 ID 。
     *
     * 用于唯一标识服务器实例。
     *
     * 请求方式为 HTTP POST ，返回纯文本，内容为服务器 ID 。
     */
    const val SERVER_ID: String = "/server/id"

    /**
     * 查询服务器名称。
     *
     * 服务端可以声明任意名称，以便客户端进行识别。
     *
     * 请求方式为 HTTP POST ，返回纯文本，内容为服务器名称。
     */
    const val SERVER_NAME: String = "/server/name"

    /**
     * 查询通信协议版本。
     *
     * 调用其他方法之前，客户端应当检查自身版本号是否与服务端版本号相匹配，避免出现兼容性问题。
     *
     * 请求方式为 HTTP POST ，返回纯文本，内容为版本号。
     */
    const val SERVER_VERSION: String = "/server/version"


    /* ----- 基础功能 ----- */

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


    /* ----- 屏幕投射 ----- */

    /**
     * 请求远程设备开启投屏转发。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBCastStartRequest] ；返回纯文本，内容为服务端音视频数据的转发端口，请求者需要使用
     * `ADBCastReceiver` 建立连接并进行解码。
     */
    const val ADB_CAST_START: String = "/adb/cast/start"

    /**
     * 请求远程设备停止投屏转发。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBCastStopRequest] ；返回纯文本。
     */
    const val ADB_CAST_STOP: String = "/adb/cast/stop"


    /* ----- 安全策略 ----- */

    /**
     * 配对第一步：客户端提交随机数承诺，服务端返回会话 ID 与服务端随机数。
     *
     * 需要 TLS 客户端证书，但客户端无需已配对，请求体为 `PairRequest` ，响应体为 `PairInitResponse` 。
     */
    const val PAIR_COMMIT: String = "/pair/commit"

    /**
     * 配对第二步：客户端公开随机数，服务端展示配对码并等待用户批准，批准后记录客户端证书指纹并返回 200 。
     *
     * 需要 TLS 客户端证书，且必须与第一步使用同一证书，请求体为 `PairRevealRequest` 。
     */
    const val PAIR_REVEAL: String = "/pair/reveal"

    /**
     * 配对状态。
     *
     * 需要已配对的客户端证书，返回 200 表示当前客户端已配对，否则返回 401 。
     */
    const val PAIR_STATUS: String = "/pair/status"
}
