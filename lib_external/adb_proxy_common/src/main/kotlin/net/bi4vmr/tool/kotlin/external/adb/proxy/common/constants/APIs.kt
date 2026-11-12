package net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants

import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceType
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.ServerInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealResponse

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
     * 请求方式为 HTTP POST，无请求参数。
     *
     * 返回 [BaseResponse] JSON 文本，数据载荷为 [ServerInfo] 。
     */
    const val SERVER_INFO: String = "/server/info"


    /* ----- 基础功能 ----- */

    /**
     * 信令消息。
     *
     * 服务端用于通告设备状态变更等事件。
     *
     * 协议为 WebSocket 。
     */
    const val ADB_EVENTS: String = "/adb/events"

    /**
     * 查询设备列表。
     *
     * 请求方式为 HTTP POST，无请求参数。
     *
     * 返回设备列表 JSON 文本。
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
     * 远程 ADB Shell 。
     *
     * 协议为 WebSocket ，请求体为 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest] 。
     */
    const val ADB_SHELL: String = "/adb/shell"

    /**
     * 获取设备属性。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBPropertyRequest] ；返回纯文本，内容为属性值。
     */
    const val ADB_PROPERTY: String = "/adb/property"

    /**
     * 获取设备名称。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBDeviceRequest] ；返回纯文本，内容为设备名称，特殊值 `Unknown` 表示设备离线等状态。
     */
    const val ADB_DEVICE_NAME: String = "/adb/device-name"

    /**
     * 获取设备类型。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBDeviceRequest] ；返回纯文本，内容为 [DeviceType] 枚举名称。
     */
    const val ADB_DEVICE_TYPE: String = "/adb/device-type"

    /**
     * 推送文件到设备。
     *
     * 协议为 WebSocket ，查询参数为 `sn` （设备序列号）、 `remote` （目标路径）、 `name` （源文件名称）与 `size` （源文件大小）。
     *
     * 客户端先以二进制帧发送文件内容，随后发送文本帧 `eof` ；任意时刻发送文本帧 `cancel` 可取消传输。服务端以文本帧返回
     * [net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.FilePushEvent] 。
     */
    const val ADB_FILE_PUSH: String = "/adb/file/push"


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
     * 请求方式为 HTTP POST ，请求体为 [PairCommitRequest] ，响应体为 [PairCommitResponse] 。
     */
    const val PAIR_COMMIT: String = "/pair/commit"

    /**
     * 配对（第二阶段）。
     *
     * 请求方式为 HTTP POST ，请求体为 [PairRevealRequest] ，响应体为 [PairRevealResponse] 。
     */
    const val PAIR_REVEAL: String = "/pair/reveal"

    /**
     * 配对（第三阶段）
     *
     * 双方显示配对码供用户核对，客户端等待服务端批准或拒绝。
     *
     * 请求方式为 HTTP POST ，无请求参数，服务端将会持续等待用户选择结果再返回响应体，内容为结果代号，可转换为 [PairResult] 。
     */
    const val PAIR_ASK: String = "/pair/ask"

    /**
     * 客户端取消配对。
     *
     * 请求方式为 HTTP POST ，无请求参数、无返回值。
     */
    const val PAIR_CANCEL: String = "/pair/cancel"

    /**
     * 客户端配对信息查询。
     *
     * 请求方式为 HTTP POST ，无请求参数、无返回值。
     *
     * - `404` 表示服务端未开启认证，客户端可以直接访问业务接口。
     * - `401` 表示服务端已开启认证且当前客户端未配对，客户端应当发起配对请求。
     * - `200` 表示服务端已开启认证且当前客户端已配对。
     */
    const val PAIR_INFO: String = "/pair/info"
}
