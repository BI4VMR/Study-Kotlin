package net.bi4vmr.study.adbmqtt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 单个ADB设备信息 */
@Serializable
data class Device(
    val serial: String,
    val state: String,
    val model: String = ""
)

/** 设备列表快照（全量，便于幂等处理） */
@Serializable
data class DeviceList(
    val ts: Long,
    val devices: List<Device>
)

/** 命令请求 */
@Serializable
data class CommandRequest(
    val reqId: String,
    /** 目标设备序列号，为空表示不指定设备 */
    val serial: String? = null,
    /** ADB参数数组，例如 ["shell", "ls"]，不含 "adb" 本身 */
    val args: List<String>,
    val timeoutMs: Long = 10_000
)

/** 命令响应 */
@Serializable
data class CommandResponse(
    val reqId: String,
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = ""
)

/** Topic定义 */
object Topics {
    fun devices(agentId: String) = "adb/$agentId/devices"
    fun status(agentId: String) = "adb/$agentId/status"
    fun cmd(agentId: String, clientId: String) = "adb/$agentId/cmd/$clientId"
    fun cmdFilter(agentId: String) = "adb/$agentId/cmd/+"
    fun resp(agentId: String, clientId: String) = "adb/$agentId/resp/$clientId"

    const val STATUS_ONLINE = "online"
    const val STATUS_OFFLINE = "offline"
}

val ProtocolJson = Json { ignoreUnknownKeys = true }
