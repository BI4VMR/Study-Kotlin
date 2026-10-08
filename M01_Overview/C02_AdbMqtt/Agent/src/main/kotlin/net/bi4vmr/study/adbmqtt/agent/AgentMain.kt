package net.bi4vmr.study.adbmqtt.agent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import net.bi4vmr.study.adbmqtt.CommandRequest
import net.bi4vmr.study.adbmqtt.CommandResponse
import net.bi4vmr.study.adbmqtt.Device
import net.bi4vmr.study.adbmqtt.DeviceList
import net.bi4vmr.study.adbmqtt.ProtocolJson
import net.bi4vmr.study.adbmqtt.Topics
import net.bi4vmr.study.adbmqtt.connectSuspend
import net.bi4vmr.study.adbmqtt.newMqttClient
import net.bi4vmr.study.adbmqtt.publishText
import net.bi4vmr.study.adbmqtt.subscribeText

/** 设备来源，后续可替换为 `adb track-devices` 实现。 */
interface DeviceSource {
    fun devices(): Flow<List<Device>>
}

/** 命令执行器，后续可替换为 ProcessBuilder 实现。 */
interface CommandExecutor {
    suspend fun execute(req: CommandRequest): CommandResponse
}

/** 模拟设备源：每隔数秒轮流增减设备。 */
class MockDeviceSource : DeviceSource {
    override fun devices(): Flow<List<Device>> = flow {
        val all = listOf(
            Device("EMULATOR-0001", "device", "Pixel_7"),
            Device("R5CT123ABCD", "device", "SM-G991B"),
            Device("0123456789", "unauthorized")
        )
        var n = 0
        while (true) {
            emit(all.take(n % (all.size + 1)))
            n++
            delay(5_000)
        }
    }
}

/** 模拟执行器：不调用真实adb，仅回显参数。 */
class MockExecutor : CommandExecutor {
    override suspend fun execute(req: CommandRequest): CommandResponse {
        delay(300)
        return CommandResponse(req.reqId, 0, stdout = "[mock] adb ${req.args.joinToString(" ")} (serial=${req.serial})")
    }
}

/**
 * 用法：AgentMain [agentId] [host] [port]
 *
 * `./gradlew :M01_Overview:C02_AdbMqtt:Agent:run --args="agent1 localhost 1883" --console=plain`
 */
fun main(args: Array<String>): Unit = runBlocking {
    val agentId = args.getOrElse(0) { "agent1" }
    val host = args.getOrElse(1) { "localhost" }
    val port = args.getOrElse(2) { "1883" }.toInt()

    val source: DeviceSource = MockDeviceSource()
    val executor: CommandExecutor = MockExecutor()

    val client = newMqttClient(
        clientId = "adb-agent-$agentId",
        host = host,
        port = port,
        willTopic = Topics.status(agentId),
        willPayload = Topics.STATUS_OFFLINE
    )
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    client.connectSuspend()
    println("Agent[$agentId] connected to $host:$port")
    client.publishText(Topics.status(agentId), Topics.STATUS_ONLINE, retain = true)

    client.subscribeText(Topics.cmdFilter(agentId)) { topic, payload ->
        val clientId = topic.substringAfterLast('/')
        scope.launch {
            val resp = handle(payload, executor)
            client.publishText(Topics.resp(agentId, clientId), ProtocolJson.encodeToString(resp))
        }
    }

    scope.launch {
        // 仅在列表变化时发布完整快照（retain，新客户端订阅即得最新状态）
        source.devices().distinctUntilChanged().collect { list ->
            val snapshot = DeviceList(System.currentTimeMillis(), list)
            client.publishText(Topics.devices(agentId), ProtocolJson.encodeToString(snapshot), retain = true)
            println("Published devices: ${list.map { it.serial }}")
        }
    }

    awaitCancellation()
}

private suspend fun handle(payload: String, executor: CommandExecutor): CommandResponse {
    val req = try {
        ProtocolJson.decodeFromString<CommandRequest>(payload)
    } catch (e: Exception) {
        return CommandResponse("", -1, stderr = "bad request: $e")
    }
    return try {
        withTimeout(req.timeoutMs) { executor.execute(req) }
    } catch (e: TimeoutCancellationException) {
        CommandResponse(req.reqId, -1, stderr = "timeout")
    } catch (e: Exception) {
        CommandResponse(req.reqId, -1, stderr = e.toString())
    }
}
