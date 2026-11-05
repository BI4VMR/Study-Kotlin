package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.wss
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.network.tls.addKeyStore
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBCastStartRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBExecRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ServerInfoResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.serializer.DisplayInfoSerializer
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
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
     * 目标主机。
     */
    @Volatile
    private var host: String,

    /**
     * 目标端口。
     *
     * 默认值为 [Constants.PORT_SERVER_DEFAULT]。
     */
    @Volatile
    private var port: Int = Constants.PORT_SERVER_DEFAULT
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
     * 是否加密连接。
     *
     * `false` 表示使用明文 HTTP ； `true` 表示使用 HTTPS 。该配置应当与服务端匹配。
     */
    @Volatile
    private var secure: Boolean = false

    /**
     * SSL 配置。
     *
     * 默认为空值，此时请求不展示与验证客户端证书；若服务器已启用配对认证，则必须配置。
     */
    @Volatile
    private var sslConfig: SSLConfig? = null

    /**
     * Ktor 客户端。
     */
    private var client: HttpClient = createClient()

    /**
     * 子任务作用域。
     */
    private var taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    /**
     * Flow : 远程连接状态。
     *
     * 通过 WebSocket 信令通道是否就绪反馈客户端与服务端之间的连通性。
     */
    private val _serviceStateFlow: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.INIT)

    /**
     * Flow : 远程连接状态。
     *
     * 通过 WebSocket 信令通道是否就绪反馈客户端与服务端之间的连通性。
     */
    val serviceStateFlow: StateFlow<ServiceState> = _serviceStateFlow

    /**
     * Flow : 服务端错误。
     */
    private val _serviceErrorFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * Flow : 服务端错误。
     */
    val errorFlow: StateFlow<Exception?> = _serviceErrorFlow

    /**
     * Flow : 远程设备列表。
     */
    private val _devicesFlow: MutableStateFlow<List<RemoteDevice>> = MutableStateFlow(emptyList())

    /**
     * Flow : 远程设备列表。
     */
    val devicesFlow: StateFlow<List<RemoteDevice>> = _devicesFlow


    /* ----- 客户端配置 ----- */

    /**
     * 初始化。
     *
     * 与服务器建立连接，连接成功后可监听设备列表变化等事件。
     *
     * 本方法不会阻塞调用线程，连接状态可通过 [serviceStateFlow] 获取。
     */
    @Synchronized
    fun init() {
        if (!canStart()) {
            logger.warn("This service is already running, ignored.")
            return
        }

        /* 重置状态 */
        _serviceErrorFlow.value = null
        _serviceStateFlow.value = ServiceState.PREPARING

        // 释放上次连接遗留的资源
        taskScope.cancel()
        client.close()
        client = createClient()
        taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        connectWebSocket()
    }

    fun getServiceState(): ServiceState = serviceStateFlow.value

    fun isServiceReady(): Boolean = (serviceStateFlow.value == ServiceState.READY)

    /**
     * 释放资源。
     *
     * 调用后可再次调用 [init] 重新连接。
     */
    @Synchronized
    fun stop() {
        if (!canStop()) {
            logger.warn("This server is not running, ignored.")
            return
        }

        taskScope.cancel()
        client.close()

        _serviceErrorFlow.value = null
        _serviceStateFlow.value = ServiceState.INIT
    }

    fun getHost(): String = host

    fun getPort(): Int = port

    /**
     * 设置目标地址和端口。
     *
     * 该配置不会影响已启动的客户端，若要刷新需终止客户端并重新启动。
     *
     * @param[host] 目标地址。
     * @param[port] 目标端口。
     */
    fun setTarget(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    fun isSecure(): Boolean = secure

    fun setSecure(secure: Boolean) {
        this.secure = secure
    }

    fun getSSLConfig(): SSLConfig? = sslConfig

    /**
     * 设置 SSL 配置。
     *
     * 该配置不会影响已启动的客户端，若要刷新需终止客户端并重新启动。
     *
     * @param[config] SSL 配置。
     */
    fun setSSLConfig(config: SSLConfig?) {
        sslConfig = config
    }


    /* ----- 服务端管理 ----- */

    /**
     * 获取服务端信息。
     *
     * @return 服务端信息。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun getServerInfo(): ServerInfoResponse {
        val url = buildURL(APIs.SERVER_INFO)
        val call = client.post(url)

        check(call.status.isSuccess()) {
            "GetServerInfo failed. Status:[${call.status}]"
        }

        val callResponse: BaseResponse<ServerInfoResponse> = call.body()
        logger.info("GetServerInfo response: $callResponse")
        check(callResponse.isSuccess()) {
            "GetServerInfo failed. Code:[${callResponse.code}] Message:[${callResponse.message}]"
        }
        return requireNotNull(callResponse.data) { "GetServerInfo failed. Data is null." }
    }

    /**
     * 获取设备列表。
     *
     * @return 设备列表。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun fetchDeviceList(): List<RemoteDevice> {
        val url = buildURL(APIs.ADB_DEVICES)
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
            val response = client.post(buildURL(APIs.ADB_EXEC_STATUS)) {
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
            val response = client.post(buildURL(APIs.ADB_EXEC_TEXT)) {
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
            val response = client.post(buildURL(APIs.ADB_DISPLAYS)) {
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


    /**
     * 将 Classpath 写入 Java 参数文件（`@argfile`）。
     *
     * Windows 的 Classpath 以 `;` 分隔，而 Windows Terminal（`wt`）会把 `;` 当作子命令分隔符，
     * 直接通过命令行传递会被拆分成大量标签页；使用参数文件可避免该问题，同时规避命令行长度限制。
     */
    private fun createBridgeArgFile(): File {
        val classpath = System.getProperty("java.class.path")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return File.createTempFile("adb-shell-bridge-", ".args").apply {
            deleteOnExit()
            writeText("-cp \"$classpath\"\n")
        }
    }

    fun openShell(sn: String) {
        taskScope.launch {
            var localServer: ServerSocket? = null
            try {
                openWebSocket("${APIs.ADB_SHELL}?sn=${sn}") {
                    localServer = ServerSocket(0)

                    val javaBin = ProcessHandle.current().info().command()
                        .orElse(File(System.getProperty("java.home"), "bin/java").path)
                    val argFile = createBridgeArgFile()
                    val bridgeCommand = listOf(
                        javaBin,
                        "@${argFile.absolutePath}",
                        ShellBridge::class.java.name,
                        localServer.localPort.toString()
                    )
                    // ProcessBuilder(listOf("konsole", "-e") + bridgeCommand)
                    //     .inheritIO()
                    //     .start()
                    ProcessBuilder(listOf("wt") + bridgeCommand)
                        .inheritIO()
                        .start()

                    localServer.accept().use { localSocket ->
                        val upstream = launch {
                            val input = DataInputStream(localSocket.getInputStream())
                            try {
                                while (true) {
                                    val frame = withContext(Dispatchers.IO) {
                                        val type = input.readByte().toInt()
                                        val data = ByteArray(input.readInt())
                                        input.readFully(data)
                                        when (type) {
                                            ShellBridge.FRAME_RESIZE -> Frame.Text("resize ${String(data)}")
                                            else -> Frame.Binary(true, data)
                                        }
                                    }
                                    send(frame)
                                }
                            } catch (_: EOFException) {
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
        val response = client.post(buildURL(APIs.ADB_CAST_START)) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }

        check(response.status.isSuccess()) {
            "Failed to start remote cast. Reason:[${response.bodyAsText()}]"
        }

        return response.bodyAsText().trim().toIntOrNull()
            ?: throw IllegalStateException("Invalid cast port returned by server.")
    }

    private fun buildURL(path: String): Url =
        URLBuilder(
            protocol = if (secure) URLProtocol.HTTPS else URLProtocol.HTTP,
            host = host,
            port = port
        )
            .apply {
                path(path)
            }
            .build()

    private suspend fun openWebSocket(path: String, block: suspend DefaultClientWebSocketSession.() -> Unit) {
        if (secure) {
            client.wss(host = host, port = port, path = path, block = block)
        } else {
            client.webSocket(host = host, port = port, path = path, block = block)
        }
    }


    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            engine {
                val ssl = sslConfig
                if (ssl == null) {
                    return@engine
                }

                https {
                    this.trustManager = PinningTrustManager(null)
                    // 向服务端出示客户端证书
                    try {
                        addKeyStore(
                            store = ssl.getKeyStore(),
                            password = ssl.keyPassword.toCharArray(),
                            alias = ssl.keyAlias
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to add keystore for client certificate. Reason:[${e.message}]")
                        e.printStackTrace()
                    }
                }
            }

            // 日志输出
            install(Logging) { level = LogLevel.INFO }
            // 自动反序列化
            install(ContentNegotiation) { json(jsonTool) }
            // WebSocket 支持
            install(WebSockets)
            // install(HttpTimeout)
        }
    }

    private fun connectWebSocket() {
        // 进入该方法前，客户端状态已在 PREPARING 。
        taskScope.launch {
            try {
                // Ktor 的 WebSocket 请求只能填写路径，不能携带 `wss://` 等前缀。
                openWebSocket(APIs.ADB_EVENTS) {
                    logger.info("Connect to [$host:$port] successfully.")
                    _serviceStateFlow.value = ServiceState.READY

                    // 循环处理接收到的事件帧
                    for (frame in incoming) {
                        // 目前控制通道仅用于发送设备列表，无需判断具体的消息类型。
                        if (frame is Frame.Text) {
                            _devicesFlow.value = jsonTool.decodeFromString(
                                ListSerializer(RemoteDevice.serializer()),
                                frame.readText()
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                logger.info("Connect with [$host:$port] job has been cancelled.")
                // 取消异常无需上报
            } catch (e: Exception) {
                logger.warn("Failed to connect server [$host:$port]. Reason:[${e.message}]")
                _serviceErrorFlow.value = e
            } finally {
                // 遇到异常或事件循环结束表示连接已断开
                _serviceStateFlow.value = ServiceState.INIT
            }
        }
    }

    // 是否可以启动服务端
    private fun canStart(): Boolean = (getServiceState() == ServiceState.INIT)

    // 是否可以终止服务端
    private fun canStop(): Boolean = (getServiceState() != ServiceState.INIT)
}
