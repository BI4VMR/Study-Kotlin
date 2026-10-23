package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.API
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBCastStartRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBExecRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.serializer.DisplayInfoSerializer
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.ServerSocket

/**
 * ADB 代理客户端。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBProxyClient(

    /**
     * 监听地址。
     */
    val host: String,

    /**
     * 监听端口。
     *
     * 默认值为 [Constant.PORT_SERVER_DEFAULT]。
     */
    val port: Int = Constant.PORT_SERVER_DEFAULT,
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBProxyClient::class.java)

    /**
     * 全局 JSON 工具。
     */
    private val jsonTool: Json = Json {
        // 解码时忽略未知字段
        ignoreUnknownKeys = true
        // 编码时对于未指明的字段填入默认值
        encodeDefaults = true
    }

    /**
     * Ktor 客户端。
     */
    private val client = HttpClient(CIO) {
        // 日志输出
        install(Logging) { level = LogLevel.INFO }
        // 自动反序列化
        install(ContentNegotiation) { json(jsonTool) }
        // WebSocket 支持
        install(WebSockets)
    }

    /**
     * 子任务作用域。
     */
    private val taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO)


    /**
     * Flow : 远程连接状态。
     *
     * 通过 WebSocket 信令通道是否就绪反馈客户端与服务端之间的连通性。
     */
    private val _serviceStateFlow: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.IDLE)

    /**
     * Flow : 远程连接状态。
     *
     * 通过 WebSocket 信令通道是否就绪反馈客户端与服务端之间的连通性。
     */
    val serviceStateFlow: StateFlow<ServiceState> = _serviceStateFlow

    /**
     * Flow : 远程设备列表。
     */
    private val _devicesFlow: MutableStateFlow<List<RemoteDevice>> = MutableStateFlow(emptyList())

    /**
     * Flow : 远程设备列表。
     */
    val devicesFlow: StateFlow<List<RemoteDevice>> = _devicesFlow


    /* ----- 状态管理 ----- */

    /**
     * 初始化。
     *
     * 与服务器建立连接，连接成功后可监听设备列表变化等事件。
     *
     * 本方法不会阻塞调用线程，连接状态可通过 [serviceStateFlow] 获取。
     */
    fun init() {
        connectWebSocket()
    }

    private fun connectWebSocket() {
        taskScope.launch {
            _serviceStateFlow.value = ServiceState.PREPARING
            try {
                // Ktor 的 WebSocket 请求只能填写路径，不能携带 `ws://` 等前缀。
                client.webSocket(host = host, port = port, path = API.ADB_EVENTS) {
                    logger.info("Connect to [$host:$port] successfully.")
                    _serviceStateFlow.value = ServiceState.READY

                    // 循环处理接收到的事件帧
                    for (frame in incoming) {
                        // 目前控制通道仅用于发送设备列表，无需判断具体的消息类型。
                        if (frame is Frame.Text) {
                            _devicesFlow.value = jsonTool.decodeFromString(
                                ListSerializer(RemoteDevice.serializer()),
                                frame.readText().also {
                                    println(it)
                                }
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                logger.info("Connect with [$host:$port] job has been cancelled.")
            } catch (e: Exception) {
                logger.warn("Failed to connect server [$host:$port]. Reason:[${e.message}]")
            } finally {
                // 遇到异常或事件循环结束表示连接已断开
                _serviceStateFlow.value = ServiceState.IDLE
            }
        }
    }

    fun isServiceReady(): Boolean = (_serviceStateFlow.value == ServiceState.READY)

    /**
     * 释放资源。
     *
     * 本方法调用后客户端实例不再可用，若需重新连接，请创建新的实例。
     */
    fun release() {
        taskScope.cancel()
        client.close()
    }


    /* ----- 远程服务端操作 ----- */

    /**
     * 获取服务端版本。
     *
     * 可以将服务端版本号与 [Constant.PROTOCOL_VERSION] 比较，防止出现兼容性问题，该策略非强制，上层应用可在初始化时执行。
     *
     * @return 版本号。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun getServerVersion(): String {
        val url = buildURL(API.VERSION)
        return client.get(url).bodyAsText()
    }

    /**
     * 获取设备列表。
     *
     * @return 设备列表。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun fetchDeviceList(): List<RemoteDevice> {
        val url = buildURL(API.ADB_DEVICES)
        return client.post(url).body()
    }

    /**
     * 在远程设备上执行 ADB 命令并获取状态码。
     *
     * 命令由服务端执行完毕后才会返回，调用协程将被挂起。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句，与本地 `ADBUtil.runForStatus()` 的规则一致。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    suspend fun runForStatus(sn: String, command: String): Int {
        return try {
            val response = client.post(buildURL(API.ADB_EXEC_STATUS)) {
                val params = ADBExecRequest(
                    sn = sn,
                    command = command
                )
                contentType(ContentType.Application.Json)
                setBody(params)
            }

            if (response.status.isSuccess()) {
                response.bodyAsText().trim().toIntOrNull() ?: -1
            } else {
                logger.warn("Failed to run command. Reason:[${response.bodyAsText()}]")
                -1
            }
        } catch (e: Exception) {
            logger.warn("Failed to run command. Reason:[${e.message}]")
            -1
        }
    }

    /**
     * 在远程设备上执行 ADB 命令并获取文本消息。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句，与本地 `ADBUtil.runForText()` 的规则一致。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    suspend fun runForText(
        sn: String,
        command: String,
        redirectError: Boolean = true,
        charset: String? = null
    ): String? {
        return try {
            val response = client.post(buildURL(API.ADB_EXEC_TEXT)) {
                contentType(ContentType.Application.Json)
                setBody(ADBExecRequest(sn, command, redirectError, charset))
            }

            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                logger.warn("Failed to run command. Reason:[${response.bodyAsText()}]")
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to run command. Reason:[${e.message}]")
            null
        }
    }

    /**
     * 在远程设备上执行 ADB 命令并获取文本行。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句，与本地 `ADBUtil.runForLines()` 的规则一致。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出的文本行。若出现异常则返回空值。
     */
    suspend fun runForLines(
        sn: String,
        command: String,
        redirectError: Boolean = true,
        charset: String? = null
    ): List<String>? {
        val text = runForText(sn, command, redirectError, charset) ?: return null
        // 与按行读取的语义保持一致：末尾换行符不产生空行。
        return text.lineSequence().toList()
    }

    /**
     * 获取远程设备的显示屏信息列表。
     *
     * @param[sn] 设备序列号。
     * @return 显示屏信息列表。若出现异常则返回空列表。
     */
    suspend fun getDisplays(sn: String): List<DisplayInfo> {
        return try {
            val response = client.post(buildURL(API.ADB_DISPLAYS)) {
                contentType(ContentType.Application.Json)
                setBody(ADBExecRequest(sn = sn))
            }

            if (response.status.isSuccess()) {
                jsonTool.decodeFromString(
                    ListSerializer(DisplayInfoSerializer),
                    response.bodyAsText()
                )
            } else {
                logger.warn("Failed to get displays. Reason:[${response.bodyAsText()}]")
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to get displays. Reason:[${e.message}]")
            emptyList()
        }
    }


    fun openShell(sn: String) {
        taskScope.launch {
            var localServer: ServerSocket? = null
            try {
                client.webSocket(
                    host = host,
                    port = port,
                    path = "${API.ADB_SHELL}?sn=${sn}"
                ) {
                    localServer = ServerSocket(0)

                    val script = "stty raw -echo; socat - TCP:127.0.0.1:${localServer.localPort}; stty sane"
                    val code = ProcessBuilder("konsole", "-e", "sh", "-c", script)
                        .inheritIO()
                        .start()
                    System.err.println("\n会话已结束（终端退出码 $code）")

                    val localSocket = localServer.accept()

                    val upstream = launch {
                        val buf = ByteArray(4096)
                        val input = localSocket.getInputStream()
                        while (true) {
                            val n = withContext(Dispatchers.IO) { input.read(buf) }
                            if (n < 0) break
                            send(Frame.Binary(true, buf.copyOf(n)))
                        }
                        close()
                    }

                    val output = localSocket.getOutputStream()
                    for (frame in incoming) {
                        if (frame is Frame.Binary) {
                            output.write(frame.readBytes())
                            output.flush()
                        }
                    }
                    upstream.cancel()
                }
            } catch (e: Exception) {
                logger.warn("Failed to open shell: ${e.message}")
            } finally {
                localServer?.close()
            }
        }
    }

    /**
     * 请求服务端为指定设备启动投屏转发。
     *
     * 请求成功后可将返回端口传入 `ADBCastReceiver.start(host, port, listener)`，
     * 由接收端连接服务端建立的投屏通道。投屏接收模块需由调用方单独引入。
     *
     * @param[sn] 服务端已连接设备的序列号。
     * @return 服务端投屏监听端口。
     * @throws[IllegalStateException] 服务端未启用投屏路由或启动投屏失败。
     */
    suspend fun startCast(
        sn: String,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = false,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitRate: Int = 128_000
    ): Int {
        /* 参数校验 */
        require(sn.isNotBlank()) { "Device SN is required!" }

        val params = ADBCastStartRequest(
            sn = sn,
            displayID = displayID,
            videoCodec = videoCodec,
            bitRate = bitRate,
            maxSize = maxSize,
            maxFPS = maxFPS,
            enableAudio = enableAudio,
            audioCodec = audioCodec,
            audioBitRate = audioBitRate
        )

        /* 发起 HTTP 请求 */
        val response = client.post(buildURL(API.ADB_CAST_START)) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }

        check(response.status.isSuccess()) {
            "Failed to start remote cast. Reason:[${response.bodyAsText()}]"
        }

        return response.bodyAsText().trim().toIntOrNull()
            ?: throw IllegalStateException("Invalid cast port returned by server.")
    }

    private fun buildURL(path: String): String = "http://$host:$port$path"
}
