package net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs.PAIR_COMMIT
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs.PAIR_REVEAL
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealRequest

/**
 * URL 定义。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object APIs {

    /* ----- 系统管理 ----- */

    /**
     * 查询服务端信息。
     *
     * 获取名称、版本等基本信息，无需鉴权。
     *
     * 请求方式为 HTTP POST ，返回 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse] ，数据载荷为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ServerInfoResponse] 。
     */
    const val SERVER_INFO: String = "/server/info"


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
     * 请求方式为 HTTP POST ，请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBExecRequest] ；返回纯文本，内容为命令的状态码。
     */
    const val ADB_EXEC_STATUS: String = "/adb/exec/status"

    /**
     * 执行 ADB Shell 命令并返回文本。
     *
     * 请求方式为 HTTP POST ，请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBExecRequest] ；返回纯文本，内容为命令的输出文本。
     */
    const val ADB_EXEC_TEXT: String = "/adb/exec/text"

    /**
     * 查询设备屏幕信息。
     *
     * 请求方式为 HTTP POST ，请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest] ；返回纯文本，内容为命令的输出文本。
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
     * 协议为 WebSocket ，请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest] 。
     */
    const val ADB_SHELL: String = "/adb/shell"


    /* ----- 屏幕投射 ----- */

    /**
     * 请求远程设备开启投屏转发。
     *
     * 请求方式为 HTTP POST ，请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBCastStartRequest] ；返回纯文本，内容为服务端音视频数据的转发端口，请求者需要使用
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
     * 配对（第一阶段）。
     *
     * 双方交换会话随机数与证书指纹的消息摘要。
     *
     * 请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitRequest] ，响应体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitResponse] 。
     */
    const val PAIR_COMMIT: String = "/pair/commit"

    /**
     * 配对（第二阶段）。
     *
     * 请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealRequest] ，响应体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealResponse] 。
     */
    const val PAIR_REVEAL: String = "/pair/reveal"

    /**
     * 配对（第三阶段）第三步：确认配对码，等待服务端用户批准或拒绝。
     *
     * 需要 TLS 客户端证书，且必须与 [PAIR_COMMIT] 、[PAIR_REVEAL] 使用同一证书。
     *
     * 请求方式为 HTTP POST ，无请求体。服务端将请求挂起，直至用户操作、客户端取消或会话超时，
     * 返回结果如下：
     * - `200` ：用户已批准；
     * - `403` ：用户拒绝；
     * - `408` ：等待超时；
     * - `409` ：会话不存在、已过期或已被取消。
     */
    const val PAIR_CONFIRM: String = "/pair/confirm"

    /**
     * 客户端取消配对。
     *
     * 请求方式为 HTTP POST 。
     */
    const val PAIR_CANCEL: String = "/pair/cancel"

    /**
     * 配对状态。
     *
     * 需要已配对的客户端证书，返回 200 表示当前客户端已配对，否则返回 401 。
     */
    const val PAIR_STATUS: String = "/pair/status"
}
