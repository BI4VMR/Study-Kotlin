package net.bi4vmr.study.adbmqtt.client

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import net.bi4vmr.study.adbmqtt.CommandRequest
import net.bi4vmr.study.adbmqtt.CommandResponse
import net.bi4vmr.study.adbmqtt.DeviceList
import net.bi4vmr.study.adbmqtt.ProtocolJson
import net.bi4vmr.study.adbmqtt.Topics
import net.bi4vmr.study.adbmqtt.connectSuspend
import net.bi4vmr.study.adbmqtt.newMqttClient
import net.bi4vmr.study.adbmqtt.publishText
import net.bi4vmr.study.adbmqtt.subscribeText
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 用法：ClientMain [agentId] [host] [port]
 * 交互输入：`[-s 序列号] adb参数...`，例如 `-s EMULATOR-0001 shell ls`；输入 `exit` 退出。
 *
 * `./gradlew :M01_Overview:C02_AdbMqtt:Client:run --args="agent1 localhost 1883" --console=plain`
 */
fun main(args: Array<String>): Unit = runBlocking {
    val agentId = args.getOrElse(0) { "agent1" }
    val host = args.getOrElse(1) { "localhost" }
    val port = args.getOrElse(2) { "1883" }.toInt()
    val clientId = "client-" + UUID.randomUUID().toString().take(8)

    val client = newMqttClient("adb-$clientId", host, port)
    val pending = ConcurrentHashMap<String, CompletableDeferred<CommandResponse>>()

    client.connectSuspend()
    println("Client[$clientId] connected, agent=$agentId")

    client.subscribeText(Topics.status(agentId)) { _, p -> println("[status] $p") }
    client.subscribeText(Topics.devices(agentId)) { _, p ->
        val list = ProtocolJson.decodeFromString<DeviceList>(p)
        println("[devices] " + list.devices.joinToString { "${it.serial}(${it.state})" }.ifEmpty { "<none>" })
    }
    client.subscribeText(Topics.resp(agentId, clientId)) { _, p ->
        val resp = ProtocolJson.decodeFromString<CommandResponse>(p)
        pending.remove(resp.reqId)?.complete(resp)
    }

    while (true) {
        val line = withContext(Dispatchers.IO) { readlnOrNull() }
        if (line == null) {
            // 标准输入不可用时仅监听，不退出
            println("stdin closed, listening only (Ctrl+C to quit)")
            awaitCancellation()
        }
        if (line.trim() == "exit") break
        val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) continue

        val serial = if (tokens[0] == "-s" && tokens.size > 2) tokens[1] else null
        val cmdArgs = if (serial != null) tokens.drop(2) else tokens
        val req = CommandRequest(UUID.randomUUID().toString(), serial, cmdArgs)

        val deferred = CompletableDeferred<CommandResponse>()
        pending[req.reqId] = deferred
        client.publishText(Topics.cmd(agentId, clientId), ProtocolJson.encodeToString(req))

        val resp = withTimeoutOrNull(req.timeoutMs + 3_000) { deferred.await() }
        pending.remove(req.reqId)
        if (resp == null) {
            println("[resp] no response (agent offline?)")
        } else {
            println("[resp] exit=${resp.exitCode}\n${resp.stdout}")
            if (resp.stderr.isNotEmpty()) println("STDERR: ${resp.stderr}")
        }
    }
    client.disconnect().await()
}
