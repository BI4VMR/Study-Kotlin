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
import net.bi4vmr.tool.kotlin.external.adb.common.constants.Propertys
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBCastStartRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBExecRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBPropertyRequest
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
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 终端桥接程序上行帧类型：窗口尺寸变化（与 `util:shell_transfer` 中的定义保持一致）。
 */
private const val SHELL_FRAME_RESIZE: Int = 1

/**
 * ADB 代理客户端。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBProxyClient(

    /**
     * 目标地址。
     */
    @Volatile
    var host: String,

    /**
     * 目标端口。
     *
     * 默认值为 [Constants.PORT_SERVER_DEFAULT]。
     */
    @Volatile
    var port: Int = Constants.PORT_SERVER_DEFAULT
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
     * 显示名称。
     *
     * 便于服务端区分每个服务端实例。
     */
    @Volatile
    var name: String = ADBProxyClient::class.java.simpleName

    /**
     * 是否启用加密连接。
     *
     * `false` 表示使用明文 HTTP ； `true` 表示使用 HTTPS 。该配置应当与服务端匹配。
     */
    @Volatile
    var enableTLS: Boolean = false

    /**
     * SSL 配置。
     *
     * 默认为空值，此时请求不展示与验证客户端证书；若服务器已启用配对认证，则必须配置。
     */
    @Volatile
    var sslConfig: SSLConfig? = null

    /**
     * 运行时配置。
     *
     * 服务启动时复制上述配置变量，以免运行过程中配置变更导致错误。
     */
    @Volatile
    private var runningConfig: RunningConfig? = null

    /**
     * Ktor 客户端。
     */
    private var client: HttpClient? = null

    /**
     * 子任务作用域。
     */
    private var taskScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)


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
        taskScope.cancel()
        client?.close()


        /* 复制关键配置 */
        val conf = RunningConfig(
            name,
            host,
            port,
            enableTLS,
            sslConfig
        )
        runningConfig = conf

        client = createClient()
        taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


        /* 初始化双向消息通道 */
        connectWebSocket()
    }

    fun getServiceState(): ServiceState = serviceStateFlow.value

    fun isServiceReady(): Boolean = (serviceStateFlow.value == ServiceState.READY)

    /**
     * 终止连接。
     *
     * 断开与服务端的连接，若要再次连接需要调用 [init] 方法。
     */
    @Synchronized
    fun stop() {
        if (!canStop()) {
            logger.warn("This server is not running, ignored.")
            return
        }

        taskScope.cancel()
        client?.close()

        _serviceErrorFlow.value = null
        _serviceStateFlow.value = ServiceState.INIT
    }

    fun getRunningConfig(): RunningConfig? = runningConfig


    /* ----- 服务端管理 ----- */

    /**
     * 获取服务端信息。
     *
     * @return 服务端信息。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun getServerInfo(): ServerInfoResponse {
        val url = buildURL(APIs.SERVER_INFO)
        val call = requireClient().post(url)

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
        return requireClient().post(url).body()
    }


    /*
     * ----- 常用命令 -----
     */

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
            val response = requireClient().post(buildURL(APIs.ADB_EXEC_STATUS)) {
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
            val response = requireClient().post(buildURL(APIs.ADB_EXEC_TEXT)) {
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
            val response = requireClient().post(buildURL(APIs.ADB_DISPLAYS)) {
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
     * 打开远程设备的交互式 Shell 。
     *
     * 需要先通过 [ShellTransferHelper.setTransferFile] 设置终端桥接程序路径。
     *
     * @param[sn] 设备序列号。
     */
    fun openShell(sn: String) {
        val bridgeJar = ShellTransferHelper.getTransferFile()
        if (bridgeJar == null || !bridgeJar.canRead()) {
            logger.warn("Failed to open shell: shell transfer file is invalid. Path:[${bridgeJar?.absolutePath}]")
            return
        }

        taskScope.launch {
            var localServer: ServerSocket? = null
            try {
                openWebSocket("${APIs.ADB_SHELL}?sn=${sn}") {
                    localServer = ServerSocket(0)

                    val javaBin = ProcessHandle.current().info().command()
                        .orElse(File(System.getProperty("java.home"), "bin/java").path)
                    val bridgeCommand = listOf(
                        javaBin,
                        "-jar",
                        bridgeJar.absolutePath,
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
                                            SHELL_FRAME_RESIZE -> Frame.Text("resize ${String(data)}")
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

    /*
     * ----- 设备信息 -----
     */

    /**
     * 获取设备属性。
     *
     * @param[key] 属性键名。
     * @param[key] 属性键名。
     * @return 属性值。若属性不存在则返回空值。
     */
    suspend fun getProperty(sn: String, key: String): String? {
        val response = requireClient().post(buildURL(APIs.ADB_PROPERTY)) {
            contentType(ContentType.Application.Json)
            setBody(ADBPropertyRequest(sn = sn, key = key))
        }

        return if (response.status.isSuccess()) {
            response.bodyAsText().trim().ifBlank { null }
        } else {
            throw IllegalStateException("Failed to get property [$key] from device [$sn]! State:[${response.status}]")
        }
    }

    /**
     * 获取设备属性。
     *
     * @param[key] 属性键名。
     * @param[key] 属性键名。
     * @return 属性值。若属性不存在则返回空值。
     */
    suspend fun getProperty(sn: String, key: String, defaultValue: String): String {
        return getProperty(sn, key) ?: defaultValue
    }

    /**
     * 获取设备制造商。
     *
     * @param[sn] 设备序列号。
     * @return 制造商。若设备未声明则为空字符串。
     */
    suspend fun getManufacturer(sn: String): String = getProperty(sn, Propertys.Key.MANUFACTURER, "")

    /**
     * 获取设备品牌。
     *
     * @param[sn] 设备序列号。
     * @return 品牌。若设备未声明则为空字符串。
     */
    suspend fun getBrand(sn: String): String = getProperty(sn, Propertys.Key.BRAND, "")

    /**
     * 获取设备型号。
     *
     * @param[sn] 设备序列号。
     * @return 型号。若设备未声明则为空字符串。
     */
    suspend fun getModel(sn: String): String = getProperty(sn, Propertys.Key.MODEL, "")

    /**
     * 获取设备 Android 系统版本。
     *
     * @param[sn] 设备序列号。
     * @return 系统版本号。若设备未声明则为空字符串。
     */
    suspend fun getOSVersion(sn: String): String = getProperty(sn, Propertys.Key.OS_VERSION, "")

    /**
     * 获取设备 Android API Level 。
     *
     * @param[sn] 设备序列号。
     * @return API Level 。若设备未声明则为空字符串。
     */
    suspend fun getAPILevel(sn: String): String = getProperty(sn, Propertys.Key.API_LEVEL, "")

    /**
     * 获取设备名称。
     *
     * 若为物理机则显示 `<品牌> <型号>` ；若为模拟器则显示模拟器名称。
     *
     * @param[sn] 设备序列号。
     * @param[default] 获取失败时的名称。默认值为 `Unknown` 。
     * @return 设备名称。
     */
    suspend fun getName(sn: String, default: String = "Unknown"): String {
        return try {
            val response = requireClient().post(buildURL(APIs.ADB_DEVICE_NAME)) {
                contentType(ContentType.Application.Json)
                setBody(ADBDeviceRequest(sn = sn))
            }

            if (response.status.isSuccess()) {
                response.bodyAsText().trim().ifBlank { default }
            } else {
                logger.warn("Failed to get device name. Reason:[${response.bodyAsText()}]")
                default
            }
        } catch (e: Exception) {
            logger.warn("Failed to get device name. Reason:[${e.message}]")
            default
        }
    }

    /**
     * 获取设备类型。
     *
     * @param[sn] 设备序列号。
     * @return 设备类型。
     * @throws[IllegalStateException] 请求失败。
     */
    suspend fun getType(sn: String): DeviceType {
        val response = requireClient().post(buildURL(APIs.ADB_DEVICE_TYPE)) {
            contentType(ContentType.Application.Json)
            setBody(ADBDeviceRequest(sn = sn))
        }

        check(response.status.isSuccess()) {
            "Failed to get type of device [$sn]! Reason:[${response.bodyAsText()}]"
        }

        val text = response.bodyAsText().trim()
        return DeviceType.entries.firstOrNull { it.name == text }
            ?: throw IllegalStateException("Invalid device type returned by server: [$text]")
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
        val response = requireClient().post(buildURL(APIs.ADB_CAST_START)) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }

        check(response.status.isSuccess()) {
            "Failed to start remote cast. Reason:[${response.bodyAsText()}]"
        }

        return response.bodyAsText().trim().toIntOrNull()
            ?: throw IllegalStateException("Invalid cast port returned by server.")
    }


    /**
     * 获取运行时配置。
     *
     * 配置不存在则抛出异常。
     *
     * @return 配置信息。
     */
    private fun requireRunningConfig(): RunningConfig {
        return runningConfig ?: throw IllegalStateException("Client is not initialized!")
    }

    /**
     * 获取客户端。
     *
     * 实例不存在则抛出异常。
     *
     * @return 客户端。
     */
    private fun requireClient(): HttpClient {
        return client ?: throw IllegalStateException("Client is not initialized! Have you call `init()` at first?")
    }

    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            engine {
                // 如果未配置 SSL 则使用默认的 SSLContext ，无需执行后续的自定义引擎配置。
                val ssl = runningConfig?.sslConfig ?: return@engine

                https {
                    // TODO
                    trustManager = PinningTrustManager(null)
                    // 双向认证：向服务端出示客户端证书
                    addKeyStore(
                        store = ssl.getKeyStore(),
                        password = ssl.keyPassword.toCharArray(),
                        alias = ssl.keyAlias
                    )
                }
            }

            // 日志输出
            install(Logging) { level = LogLevel.ALL }
            // 自动反序列化
            install(ContentNegotiation) { json(jsonTool) }
            // WebSocket 支持
            install(WebSockets)
            // TODO
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

    private fun buildURL(path: String): Url =
        URLBuilder(
            protocol = if (requireRunningConfig().enableTLS) URLProtocol.HTTPS else URLProtocol.HTTP,
            host = host,
            port = port
        )
            .apply {
                path(path)
            }
            .build()

    private suspend fun openWebSocket(path: String, block: suspend DefaultClientWebSocketSession.() -> Unit) {
        if (requireRunningConfig().enableTLS) {
            requireClient().wss(host = host, port = port, path = path, block = block)
        } else {
            requireClient().webSocket(host = host, port = port, path = path, block = block)
        }
    }
}
