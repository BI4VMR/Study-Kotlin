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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import net.bi4vmr.tool.java.common.base.file.FilePathUtil
import net.bi4vmr.tool.java.common.base.file.FileUtil
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.java.common.base.system.SystemUtil
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFolderTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.constants.Intents
import net.bi4vmr.tool.kotlin.external.adb.common.constants.Propertys
import net.bi4vmr.tool.kotlin.external.adb.common.enums.AndroidVersion
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.exceptions.RPCException
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBOutputParser
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.FileTransferEvent
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.ServerInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBCastStartRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBExecRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBFileTransferRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBPropertyRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.serializer.DisplayInfoSerializer
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminal
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.TerminalUtil
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminals
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.ServerSocket
import kotlin.coroutines.EmptyCoroutineContext

/**
 * TODO
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
     * 终端桥接程序 JAR 文件。
     *
     * 用于转发远程设备终端到本地终端程序。
     */
    @Volatile
    var shellBridgeProgram: File = FileUtil.FILE_INVALID

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
     * `false` 表示使用明文 HTTP；`true` 表示使用 HTTPS。该配置应当与服务端匹配。
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
     * Flow：远程连接状态。
     *
     * 通过 WebSocket 信令通道是否就绪反馈客户端与服务端之间的连通性。
     */
    private val _serviceStateFlow: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.INIT)

    /**
     * Flow：远程连接状态。
     *
     * 通过 WebSocket 信令通道是否就绪反馈客户端与服务端之间的连通性。
     */
    val serviceStateFlow: StateFlow<ServiceState> = _serviceStateFlow

    /**
     * Flow：服务端错误。
     */
    private val _serviceErrorFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * Flow：服务端错误。
     */
    val errorFlow: StateFlow<Exception?> = _serviceErrorFlow

    /**
     * Flow：远程设备列表。
     */
    private val _devicesFlow: MutableStateFlow<List<RemoteDevice>> = MutableStateFlow(emptyList())

    /**
     * Flow：远程设备列表。
     */
    val devicesFlow: StateFlow<List<RemoteDevice>> = _devicesFlow


    /* ----- 客户端管理 ----- */

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
        connectEventsWS()
    }

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

    /**
     * 获取服务端状态。
     *
     * @return 服务端状态。
     */
    fun getServiceState(): ServiceState = serviceStateFlow.value

    /**
     * 判断服务端是否就绪。
     *
     * @return `true` 表示服务端已就绪；`false` 表示服务端未就绪。
     */
    fun isServiceReady(): Boolean = (serviceStateFlow.value == ServiceState.READY)

    /**
     * 判断是否可以启动服务。
     *
     * @return `true` 表示可以调用 [init] 方法；`false` 表示已启动，调用 [init] 方法将被忽略。
     */
    fun canStart(): Boolean {
        val state = getServiceState()
        return (state == ServiceState.INIT) || (state == ServiceState.ERROR)
    }

    /**
     * 判断是否可以停止服务。
     *
     * @return `true` 表示可以调用 [stop] 方法；`false` 表示未启动，调用 [stop] 方法将被忽略。
     */
    fun canStop(): Boolean = (getServiceState() == ServiceState.READY)


    /* ----- 基础功能 ----- */

    /**
     * 获取服务端信息。
     *
     * @return 服务端信息。
     * @throws[ConnectException] 建立连接失败。
     * @throws[RPCException] 远程调用失败。
     */
    suspend fun getServerInfo(): ServerInfo {
        val url = buildURL(APIs.SERVER_INFO)
        val call = requireClient().post(url)

        if (!call.status.isSuccess()) {
            throw RPCException("GetServerInfo failed. HTTP Status:[${call.status}]")
        }

        val callResponse: BaseResponse<ServerInfo> = call.body()
        if (callResponse.isFailure()) {
            throw RPCException("GetServerInfo failed. Code:[${callResponse.code}] Message:[${callResponse.message}]")
        }

        return requireNotNull(callResponse.data) { "GetServerInfo failed, Data is null!" }
    }

    /**
     * 获取设备列表。
     *
     * @return 设备列表。
     * @throws[ConnectException] 建立连接失败。
     * @throws[RPCException] 远程调用失败。
     */
    suspend fun getDeviceList(): List<RemoteDevice> {
        val url = buildURL(APIs.ADB_DEVICES)
        val call = requireClient().post(url)

        if (!call.status.isSuccess()) {
            throw RPCException("GetDeviceList failed. HTTP Status:[${call.status}]")
        }

        return call.body()
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
     * @return 状态码。若出现异常则返回 `-1`。
     */
    suspend fun runForStatus(sn: String, command: String): Int {
        logger.debug("RunForStatus. SN:[$sn] Command:[$command]")
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
                logger.warn("RunForStatus - HTTP status is fail. Code:[${response.status}] Body:[${response.bodyAsText()}]")
                -1
            }
        } catch (e: Exception) {
            logger.warn("RunForStatus - Failed to run command. Reason:[${e.message}]")
            -1
        }
    }

    /**
     * 在远程设备上执行 ADB 命令并获取文本消息。
     *
     * @param[sn] 设备序列号。
     * @param[command] 命令语句，与本地 `ADBUtil.runForText()` 的规则一致。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    suspend fun runForText(
        sn: String,
        command: String,
        redirectError: Boolean = true
    ): String? {
        return try {
            val response = requireClient().post(buildURL(APIs.ADB_EXEC_TEXT)) {
                contentType(ContentType.Application.Json)
                setBody(ADBExecRequest(sn, command, redirectError))
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
     * @return 命令输出的文本行。若出现异常则返回空值。
     */
    suspend fun runForLines(
        sn: String,
        command: String,
        redirectError: Boolean = true
    ): List<String>? {
        val text = runForText(sn, command, redirectError) ?: return null
        // 与按行读取的语义保持一致：末尾换行符不产生空行。
        return text.lineSequence().toList()
    }

    /**
     * 打开交互式 ADB Shell。
     *
     * 使用前需先通过 [shellBridgeProgram] 设置终端桥接程序路径，并确保当前系统已安装至少一款受 [TerminalUtil] 支持的终端程序。
     *
     * @param[sn] 设备序列号。
     * @param[terminal] 终端程序，可以传入 [Terminals] 内置枚举，也可以传入 [Terminal] 接口自定义实现。默认值为空，此时使用默认终端。
     * @param[newWindow] 是否在新窗口中打开。默认值为是。部分终端程序不支持标签页，即使该参数设为否也会在新窗口中打开。
     * @param[keep] 初始命令执行完毕是否保持窗口。本工具用于打开 GUI 终端模拟器，通常都应当设为 `true` 以便用户查看结果；如果只需要执行
     * 命令并在程序内部获取结果，应当使用无界面的 [CLIUtil.run] 系列方法。初始命令不存在时通常会进入交互式环境，窗口保持开启，此时该参数无效。
     * @throws[IllegalArgumentException] [shellBridgeProgram] 无效。
     * @throws[IllegalStateException] [TerminalUtil] 自动侦测终端程序失败，没有可用的终端程序。
     * @throws[IOException] 指定的终端程序不存在。
     */
    fun openShell(
        sn: String,
        terminal: Terminal? = null,
        newWindow: Boolean = true
    ) {
        val bridgeJAR = shellBridgeProgram
        if (!bridgeJAR.canRead()) {
            throw IllegalArgumentException("Shell bridge program is invalid! Path:[${bridgeJAR.absolutePath}]")
        }

        taskScope.launch {
            var localServer: ServerSocket? = null
            try {
                openWebSocket("${APIs.ADB_SHELL}?sn=${sn}") {
                    /*
                     * `shellBridge` 直连服务端 WebSocket 需要处理 TLS 与认证，比较麻烦，因此开启本地临时端口，Ktor 将解密后的裸数据
                     * 传入该端口，`shellBridge` 与临时端口通信即可。
                     */
                    localServer = ServerSocket(0)

                    // 复用当前进程的 JRE 环境启动桥接程序
                    val javaExec = "${SystemUtil.getJREPath()}${File.separator}bin/java"
                    val bridgeCommand =
                        "\"$javaExec\" -jar \"${bridgeJAR.absolutePath}\" 127.0.0.1 ${localServer.localPort}"
                    TerminalUtil.launch(terminal, bridgeCommand, newWindow, true)

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

    /**
     * 切换为 Root 权限。
     *
     * @param[sn] 设备序列号。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    suspend fun root(sn: String): Int =
        runForStatus(sn, "root")

    /**
     * 重启。
     *
     * @param[sn] 设备序列号。
     * @param[option] 附加命令，可以携带 `recovery`、`fastboot`、`bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    suspend fun reboot(sn: String, option: String = ""): Int =
        runForStatus(sn, "reboot $option".trim())


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
        logger.debug("GetName. SN:[$sn] Default:[$default]")
        return try {
            /* 发起 HTTP 请求 */
            val httpResponse = requireClient().post(buildURL(APIs.ADB_DEVICE_NAME)) {
                contentType(ContentType.Application.Json)
                setBody(ADBDeviceRequest(sn))
            }

            /* 检查 HTTP 响应 */
            check(httpResponse.status.isSuccess()) {
                "Failed to get [$sn] name! Reason:[${httpResponse.bodyAsText()}]"
            }

            /* 检查业务结果 */
            val response: BaseResponse<String> = httpResponse.body()
            check(response.isSuccess()) {
                "Failed to get [$sn] name! Reason:[${response.message}]"
            }

            response.requireData()
        } catch (e: Exception) {
            logger.warn("GetName - Failed to get [$sn] name! Reason:[${e.message}]")
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
    suspend fun getType(sn: String): ConnectionType {
        logger.debug("GetType. SN:[$sn]")
        /* 发起 HTTP 请求 */
        val httpResponse = requireClient().post(buildURL(APIs.ADB_DEVICE_TYPE)) {
            contentType(ContentType.Application.Json)
            setBody(ADBDeviceRequest(sn))
        }

        /* 检查 HTTP 响应 */
        check(httpResponse.status.isSuccess()) {
            "Failed to get [$sn] type! Reason:[${httpResponse.bodyAsText()}]"
        }

        /* 检查业务结果 */
        val response: BaseResponse<String> = httpResponse.body()
        check(response.isSuccess()) {
            "Failed to get [$sn] type! Reason:[${response.message}]"
        }

        val enumName = response.requireData()
        return ConnectionType.entries.firstOrNull { it.name == enumName }
            ?: throw IllegalStateException("Invalid device type name from server. Type:[$enumName]")
    }


    /*
     * ----- 图形界面 -----
     */

    /**
     * 获取显示屏列表。
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
     * 获取显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回空值。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID。默认为空值，表示默认显示屏。
     * @return 分辨率信息。若出现异常或显示屏不存在则返回空值。
     */
    suspend fun getDisplaySize(sn: String, displayID: String = "0"): DisplaySize? {
        val version = AndroidVersion.fromAPILevel(ADBCLIUtil.getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return null
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm size"
            }
        } else {
            "wm size -d $displayID"
        }

        val lines = runForLines(sn, cmd) ?: return null
        return ADBOutputParser.parseWMSize(lines)
    }

    /**
     * 设置显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[width] 宽度。
     * @param[height] 高度。
     * @param[displayID] 显示屏逻辑 ID。默认为空值，表示默认显示屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    suspend fun setDisplaySize(sn: String, width: Int, height: Int, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(ADBCLIUtil.getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm size ${width}x$height"
            }
        } else {
            "wm size ${width}x$height -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 还原显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID。默认为空值，表示默认显示屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    suspend fun resetDisplaySize(sn: String, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(ADBCLIUtil.getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm size reset"
            }
        } else {
            "wm size reset -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 获取显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回空值。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID。默认为空值，表示默认显示屏。
     * @return 密度信息。若出现异常则返回空值。
     */
    suspend fun getDisplayDensity(sn: String, displayID: String = "0"): DisplayDensity? {
        val version = AndroidVersion.fromAPILevel(ADBCLIUtil.getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return null
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm density"
            }
        } else {
            "wm density -d $displayID"
        }

        val lines = runForLines(sn, cmd) ?: return null
        return ADBOutputParser.parseWMDensity(lines)
    }

    /**
     * 设置显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[density] 密度。
     * @param[displayID] 显示屏逻辑 ID。默认为空值，表示默认显示屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    suspend fun setDisplayDensity(sn: String, density: Int, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(ADBCLIUtil.getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm density $density"
            }
        } else {
            "wm density $density -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 还原显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏逻辑 ID。默认为空值，表示默认显示屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    suspend fun resetDisplayDensity(sn: String, displayID: String = "0"): Boolean {
        val version = AndroidVersion.fromAPILevel(ADBCLIUtil.getAPILevel(sn))
        val cmd = if (version.getAPILevelMajor() < AndroidVersion.V_10.getAPILevelMajor()) {
            if (displayID != "0") {
                // Android 10 以下版本不支持调整主屏以外的其他屏幕
                return false
            } else {
                // Android 10 以下版本不支持 `-d` 参数，该命令只能调整主屏参数。
                "wm density reset"
            }
        } else {
            "wm density reset -d $displayID"
        }

        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }

    /**
     * 获取 Activity 列表。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认为空值，表示列出所有显示屏的 Activity。
     * @return Activity 列表。命令执行失败时返回空列表。每个显示屏内按照从顶层到底层的顺序排列。
     */
    suspend fun getActivities(sn: String, displayID: String? = null): List<ActivityInfo> {
        val lines = runForLines(sn, "dumpsys activity activities") ?: emptyList()
        val list = ADBOutputParser.parseDumpActivities(lines)
        return if (displayID == null) list else list.filter { it.displayID == displayID }
    }

    /**
     * 获取 Window 列表。
     *
     * @param[sn] 设备序列号。
     * @param[displayID] 显示屏 ID。默认为空值，表示列出所有显示屏的 Window。
     * @return Window 列表。命令执行失败时返回空列表。每个显示屏内按照从顶层到底层的顺序排列。
     */
    suspend fun getWindows(sn: String, displayID: String? = null): List<WindowInfo> {
        val lines = runForLines(sn, "dumpsys window windows") ?: emptyList()
        val list = ADBOutputParser.parseDumpWindows(lines)
        return if (displayID == null) list else list.filter { it.displayID == displayID }
    }

    /**
     * 获取全局焦点信息。
     *
     * Android 9 及更低版本多屏 API 不完善，焦点所在屏幕始终为 `0`。
     *
     * @param[sn] 设备序列号。
     * @return 全局焦点信息。
     */
    suspend fun getGlobalFocusInfo(sn: String): FocusInfo {
        val cmd = """
                dumpsys window | grep -iE 'Display: mDisplayId=|mCurrentFocus=|mFocusedApp=|FocusedWindow=|mTopFocusedDisplayId='
                """.trimIndent()
        val lines = runForLines(sn, cmd) ?: return FocusInfo()
        val result = ADBOutputParser.parseGlobalFocusInfo(lines)

        // Android 9 及更低版本的 Activity 信息没有焦点信息，回退到通过 Window 信息获取。
        if (result.windowHash != "-") return result
        val linesFallback = runForLines(sn, "dumpsys window windows") ?: return result
        return ADBOutputParser.parseGlobalFocusInfoV9L(linesFallback)
    }


    /*
     * ----- 文件系统 -----
     */

    @JvmOverloads
    suspend fun pushFile(
        sn: String,
        local: String,
        remote: String,
        segmentSize: Int = Constants.FILE_SEGMENT_SIZE,
        refreshMediaDB: Boolean = true,
        listener: ADBFileTransferListener? = null
    ) {
        /* 参数校验 */
        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (segmentSize <= 0) {
            throw IllegalArgumentException("Segment size must > 0! Value:[$segmentSize]")
        }

        val localFile = File(local)
        if (!localFile.canRead()) {
            throw FileNotFoundException("Input path not exist or readable! Path:[${local}]")
        }

        if (!localFile.isFile) {
            throw IllegalStateException("Input is not file! Path:[${local}]")
        }


        openWebSocket(APIs.ADB_FILES_PUSH_FILE) {
            /* 发送首帧：指明文件相关属性 */
            val request = ADBFileTransferRequest(sn, local, remote)
            outgoing.send(Frame.Text(jsonTool.encodeToString(request)))


            /*
             * Ktor 的 WebSocket 收发 Channel 缓冲区默认为无限大，如果读取大文件时接收端处理缓慢，发送端内存将逐渐被填满。
             *
             * 我们通过缓冲区容量为 1 的 Channel 限制发送速率，每当收到接收端处理完毕上一个分片时，再唤醒发送协程转发下一个分片。
             */
            val controlChannel = Channel<Unit>(Channel.CONFLATED)

            // 用户取消操作时，发送消息通知接收端。
            listener?.setCancelNotifier {
                val event = FileTransferEvent<Unit>(FileTransferEvent.Type.CANCEL.code)
                outgoing.trySend(
                    Frame.Text(jsonTool.encodeToString(event))
                )
            }

            val totalSize: Long = localFile.length()
            listener?.onStart(totalSize)
            // 远程传输时以分片计算进度
            var transferredSegment = 0

            /* 上行通道：文件数据 */
            val uploadJob = launch(Dispatchers.IO) {
                localFile.inputStream().use { input ->
                    val buffer = ByteArray(segmentSize)
                    while (listener?.isCancelled() != true) {
                        val count = input.read(buffer)
                        // 到达文件末尾，通知接收端结束并退出循环。
                        if (count < 0) {
                            val event = FileTransferEvent<Unit>(FileTransferEvent.Type.EOF.code)
                            sendEvent(event)
                            break
                        }

                        if (count == 0) continue

                        // 读取到有效数据，转发给接收端。
                        send(Frame.Binary(true, buffer.copyOf(count)))

                        // 挂起当前协程，等待接收端处理完毕当前分片再进入下一轮循环。
                        val channelResult = controlChannel.receiveCatching()
                        // 如果读到空值，表示接收端已关闭通道，传输结束，退出循环。
                        if (channelResult.getOrNull() == null) {
                            break
                        }
                    }
                }
            }


            /* 下行通道：控制信令 */
            for (frame in incoming) {
                // 目前下行通道仅使用文本帧，忽略其他类型。
                if (frame !is Frame.Text) {
                    continue
                }

                val event = receiveEvent(frame.readText())
                val type = FileTransferEvent.Type.fromCode(event.type)
                when (type) {
                    /* 分片处理完毕 */
                    FileTransferEvent.Type.SEGMENT_OK -> {
                        transferredSegment++
                        // 通知监听者当前进度
                        val transferredSize: Long =
                            (segmentSize * transferredSegment.toLong()).coerceAtMost(totalSize)
                        if (totalSize <= 0) {
                            listener?.onProgress(transferredSize, Double.NaN)
                        } else {
                            val percent = transferredSize / totalSize.toDouble()
                            listener?.onProgress(transferredSize, percent)
                        }
                        // 通知上行通道继续发送下一个分片
                        controlChannel.trySend(Unit)
                    }
                    /* 接收端出现错误 */
                    FileTransferEvent.Type.ERROR -> {
                        val e = RPCException(event.requireData().toString())
                        throw e
                    }

                    else -> Unit
                }
            }

            // `incoming` 循环结束表示服务端已断开连接，清理相关资源。
            controlChannel.close()
            uploadJob.cancel()
        }

        if (refreshMediaDB) {
            val remoteParent = FilePathUtil.getParent(remote)
            mediaScan(sn, remoteParent)
        }
    }

    /**
     * 推送目录到设备。
     *
     * 递归传输目录内的所有文件（包括空目录），支持获取每个文件的状态、总体进度以及随时取消。
     *
     * @param[sn] 设备序列号。
     * @param[local] 源目录（PC 端）。
     * @param[remote] 目标路径（Android 端）。如果以 `/` 结尾则表示将源目录写入该目录并保持原名，否则表示源目录的内容写入
     * 路径指定的目录。
     * @param[segmentSize] 分片大小（字节）。
     * @param[listener] 传输进度监听器。
     */
    @JvmOverloads
    suspend fun pushFolder(
        sn: String,
        local: String,
        remote: String,
        segmentSize: Int = Constants.FILE_SEGMENT_SIZE,
        listener: ADBFolderTransferListener? = null
    ) {
        /* 参数校验 */
        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (segmentSize <= 0) {
            throw IllegalArgumentException("Segment size must > 0! Value:[$segmentSize]")
        }

        val localFolder = File(local)
        if (!localFolder.canRead()) {
            throw FileNotFoundException("Input path not exist or readable! Path:[${local}]")
        }

        if (!localFolder.isDirectory) {
            throw IllegalStateException("Input is not directory! Path:[${local}]")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteIsDir = FilePathUtil.isDirectory(remote)
        val remotePath = if (remoteIsDir) {
            "$remote${localFolder.name}"
        } else {
            remote
        }


        /* 计算文件总数 */
        var totalCount = 0
        localFolder.walkTopDown().forEach {
            if (it.isFile) totalCount++
        }
        listener?.onStart(totalCount)

        var currentIndex = 0
        var currentPath = FileUtil.PATH_INVALID
        // 单个文件传输任务监听器
        val fileListener = object : ADBFileTransferListener() {

            override fun onStart(size: Long) {
                currentIndex++

                // 只有目录时，以 `NaN` 通告进度。
                if (totalCount <= 0) {
                    listener?.onProgress(currentIndex, Double.NaN)
                } else {
                    val progress = currentIndex.toDouble() / totalCount.toDouble()
                    listener?.onProgress(currentIndex, progress)
                }

                listener?.onFileStart(currentPath, size)
            }

            override fun onProgress(size: Long, progress: Double) {
                listener?.onFileProgress(size, progress)
            }
        }

        // 收到取消指令时，通知正在传输的任务。
        listener?.setCancelNotifier {
            fileListener.notifyCancel()
        }


        /* 深度优先遍历源目录，处理每个元素。 */
        for (entry in localFolder.walkTopDown()) {
            if (listener?.isCancelled() == true) {
                return
            }

            // 获取当前文件相对于源目录的路径，并拼接在目标路径之后。
            val relative = entry.relativeTo(localFolder).invariantSeparatorsPath
            if (relative.isBlank()) continue
            val target = "$remotePath/$relative"

            // 创建目录
            if (entry.isDirectory) {
                runForStatus(sn, "mkdir -p '$target'")
                continue
            }

            // 跳过非普通文件的特殊文件节点
            if (!entry.isFile) continue

            currentPath = entry.absolutePath
            pushFile(sn, entry.absolutePath, target, segmentSize, false, fileListener)
            // 用户取消时不再通知文件传输结束。
            if (listener?.isCancelled() == true) return
            listener?.onFileEnd()
        }

        // 刷新媒体数据库
        val scanCmd = "am broadcast -a ${Intents.Action.MEDIA_SCAN} -d file:/$remote"
        runForStatus(sn, scanCmd)

        listener?.onEnd()
    }

    /**
     * 从设备拉取文件。
     *
     * 服务端直接读取设备端文件的数据流并分段转发，不会在代理设备上生成临时文件。
     *
     * @param[sn] 设备序列号。
     * @param[remote] 源路径（Android 端）。
     * @param[local] 目标路径（PC 端）。如果以 `/` 结尾表示将文件写入该目录并保持原名，否则表示使用路径指定的文件名称。
     * @param[listener] 传输进度监听器。
     */
    @JvmOverloads
    suspend fun pullFile(
        sn: String,
        remote: String,
        local: String,
        listener: ADBFileTransferListener? = null
    ) {
        /* 参数校验 */
        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        // 检查设备上是否存在该文件
        if (runForStatus(sn, "test -f '$remote'") != 0) {
            throw FileNotFoundException("Remote file not exist or is directory! Path:[$remote]")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteFileName = FilePathUtil.getFileName(remote)
        val localFile = File(local)
        val localPath = if (FilePathUtil.isDirectory(local)) {
            /* 本地路径指向目录 */
            localFile.mkdirs()
            // 检查目录是否创建成功
            if (!localFile.canWrite()) {
                throw FileNotFoundException("Local path not exist or writeable! Path:[${local}]")
            }
            // 返回 `<本地目录>/<远程文件名称>`
            "${localFile.absolutePath}${File.separator}${remoteFileName}"
        } else {
            /* 本地路径指向文件 */
            val parent = localFile.absoluteFile.parentFile
            // 创建父目录
            parent.mkdirs()
            if (!parent.canWrite()) {
                throw FileNotFoundException("Local path not exist or writeable! Path:[${parent}]")
            }
            // 返回本地文件路径
            localFile.absolutePath
        }

        // 读取文件大小，用于计算进度。
        val fileSize: Long =
            runForText(sn, "stat -c %s '$remote'", false)?.trim()?.toLongOrNull() ?: 0L

        val targetFile = File(localPath)

        openWebSocket(APIs.ADB_FILES_PULL_FILE) {
            /* 发送首帧：指明文件相关属性 */
            val request = ADBFileTransferRequest(sn, targetFile.absolutePath, remote)
            outgoing.send(Frame.Text(jsonTool.encodeToString(request)))

            // 用户取消操作时，发送消息通知服务端。
            listener?.setCancelNotifier {
                val event = FileTransferEvent<Unit>(FileTransferEvent.Type.CANCEL.code)
                outgoing.trySend(
                    Frame.Text(jsonTool.encodeToString(event))
                )
            }

            listener?.onStart(fileSize)
            var transferredSize: Long = 0L
            var eofReceived = false

            /* 下行：接收文件数据并写入本地文件 */
            val output = targetFile.outputStream()
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> {
                            val data = frame.readBytes()
                            // 用户取消后不再写入剩余数据，但仍回复确认，保证服务端能够继续处理取消指令。
                            if (listener?.isCancelled() != true) {
                                output.write(data)
                                transferredSize += data.size

                                if (fileSize <= 0) {
                                    listener?.onProgress(transferredSize, Double.NaN)
                                } else {
                                    val progress = transferredSize.toDouble() / fileSize.toDouble()
                                    listener?.onProgress(transferredSize, progress)
                                }
                            }

                            // 通知服务端继续发送下一个分片
                            val event = FileTransferEvent<Unit>(FileTransferEvent.Type.SEGMENT_OK.code)
                            sendEvent(event)
                        }
                        is Frame.Text -> {
                            val event = receiveEvent(frame.readText())
                            when (FileTransferEvent.Type.fromCode(event.type)) {
                                // 已到达文件末尾
                                FileTransferEvent.Type.EOF -> {
                                    eofReceived = true
                                    break
                                }
                                // 服务端出现错误
                                FileTransferEvent.Type.ERROR -> {
                                    throw RPCException(event.requireData().toString())
                                }
                                else -> Unit
                            }
                        }
                        else -> Unit
                    }
                }
            } finally {
                output.close()
            }

            if (eofReceived) {
                if (listener?.isCancelled() != true) {
                    listener?.onEnd()
                }
            } else if (listener?.isCancelled() != true) {
                throw IOException("File pull connection closed unexpectedly! Path:[$remote]")
            }
        }
    }

    /**
     * 从设备拉取目录。
     *
     * 递归传输目录内的所有文件（包括空目录），支持获取每个文件的状态、总体进度以及随时取消。
     *
     * 远程目录结构通过 [runForLines] 获取，文件通过 [pullFile] 逐个拉取。
     *
     * @param[sn] 设备序列号。
     * @param[remote] 源目录（Android 端）。
     * @param[local] 目标路径（PC 端）。如果以 `/` 结尾则表示将源目录写入该目录并保持原名，否则表示源目录的内容写入路径指定
     * 的目录。
     * @param[listener] 传输进度监听器。
     */
    @JvmOverloads
    suspend fun pullFolder(
        sn: String,
        remote: String,
        local: String,
        listener: ADBFolderTransferListener? = null
    ) {
        /* 参数校验 */
        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        // 检查设备上是否存在该目录
        if (runForStatus(sn, "test -d '$remote'") != 0) {
            throw FileNotFoundException("Remote directory not exist or is file! Path:[$remote]")
        }

        // 移除源路径末尾可能存在的 `/`。
        val remoteRoot = remote.trimEnd('/').ifEmpty { "/" }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val localRoot = if (FilePathUtil.isDirectory(local)) {
            File(local, FilePathUtil.getFileName(remoteRoot))
        } else {
            File(local)
        }

        localRoot.mkdirs()
        if (!localRoot.canWrite()) {
            throw FileNotFoundException("Local path is not writeable! Path:[$localRoot]")
        }

        /* 获取源目录中的所有目录与文件 */
        val dirs: List<String> = runForLines(sn, "find '$remoteRoot' -type d", false)
            ?.filter { it.isNotBlank() }
            ?: throw IOException("Can not access remote directory! Path:[$remoteRoot]")
        val files: List<String> = runForLines(sn, "find '$remoteRoot' -type f", false)
            ?.filter { it.isNotBlank() }
            ?: throw IOException("Can not access remote directory! Path:[$remoteRoot]")

        // 获取当前路径相对于参考目录的相对路径
        fun relativeOf(path: String): String =
            path.removePrefix(remoteRoot).trimStart('/')


        /* 在本地创建目录结构（包括空目录） */
        dirs.forEach {
            File(localRoot, relativeOf(it)).mkdirs()
        }

        val totalCount = files.size
        listener?.onStart(totalCount)

        var currentIndex = 0
        var currentPath = FileUtil.PATH_INVALID
        // 单个文件传输任务监听器
        val fileListener = object : ADBFileTransferListener() {

            override fun onStart(size: Long) {
                currentIndex++

                // 只有目录时，以 `NaN` 通告进度。
                if (totalCount <= 0) {
                    listener?.onProgress(currentIndex, Double.NaN)
                } else {
                    val progress = currentIndex.toDouble() / totalCount.toDouble()
                    listener?.onProgress(currentIndex, progress)
                }

                listener?.onFileStart(currentPath, size)
            }

            override fun onProgress(size: Long, progress: Double) {
                listener?.onFileProgress(size, progress)
            }
        }

        // 收到取消指令时，通知正在传输的任务。
        listener?.setCancelNotifier {
            fileListener.notifyCancel()
        }


        /* 依次下载每个文件，保持相对路径。 */
        for (remoteFile in files) {
            if (remoteFile.isBlank()) continue
            if (listener?.isCancelled() == true) {
                return
            }

            currentPath = remoteFile
            val localPath = File(localRoot, relativeOf(remoteFile)).absolutePath
            pullFile(sn, remoteFile, localPath, fileListener)
            // 用户取消时不再通知文件传输结束。
            if (listener?.isCancelled() == true) return
            listener?.onFileEnd()
        }

        listener?.onEnd()
    }

    /**
     * 触发媒体扫描。
     *
     * @param[sn] 设备序列号。
     * @param[path] 扫描路径。默认值为根目录 `/`。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    suspend fun mediaScan(sn: String, path: String = "/"): Boolean {
        val cmd = "am broadcast -a ${Intents.Action.MEDIA_SCAN} -d 'file:/$path'"
        val status = runForStatus(sn, cmd)
        return CLIUtil.isSuccess(status)
    }


    /*
     * ----- 软件包管理 -----
     */

    /**
     * 获取软件包列表。
     *
     * @param[sn] 设备序列号。
     * @param[type] 类型筛选条件。默认值为 [PackageType.ALL]，不设筛选条件。
     * @return 包名列表。永不为空值，命令执行异常时返回空列表。
     */
    @JvmOverloads
    suspend fun listPackages(sn: String, type: PackageType = PackageType.ALL): List<String> {
        val lines = runForLines(sn, "pm list package ${type.cmdArgs}") ?: return emptyList()
        return ADBOutputParser.parsePMListPackageOutput(lines)
    }


    /*
     * ----- 远程控制 -----
     */

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


    /*
     * ----- 内部方法 -----
     */

    /**
     * 获取运行时配置。
     *
     * @return 配置信息。
     * @throws[IllegalStateException] 配置不存在。
     */
    private fun requireRunningConfig(): RunningConfig {
        return runningConfig ?: throw IllegalStateException("Client is not initialized!")
    }

    /**
     * 创建 HTTP 客户端实例。
     *
     * @return 客户端实例。
     */
    private fun createClient(): HttpClient {
        return HttpClient(CIO) {
            engine {
                // 如果未配置 SSL 则直接返回，无需配置双向证书验证。
                val ssl = runningConfig?.sslConfig ?: return@engine

                https {
                    // 信任所有服务端证书
                    trustManager = ByPassTrustManager()
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

    /**
     * 获取 HTTP 客户端。
     *
     * @return 客户端实例。
     * @throws[IllegalStateException] 未初始化。
     */
    private fun requireClient(): HttpClient {
        return client ?: throw IllegalStateException("Client is not initialized! Have you call `init()` at first?")
    }

    /**
     * 初始化服务端事件 WebSocket 通道。
     */
    private fun connectEventsWS() {
        taskScope.launch {
            try {
                // Ktor 的 WebSocket 请求只能填写路径，不能携带 `wss://` 等前缀。
                openWebSocket(APIs.ADB_EVENTS) {
                    logger.info("Connect to [$host:$port] successfully.")
                    // 进入该方法前，客户端状态应在 `PREPARING`，连接建立后标记为就绪。
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
                _serviceStateFlow.value = ServiceState.ERROR
                _serviceErrorFlow.value = e
            }
        }
    }

    /**
     * 构建请求 URL。
     *
     * `http://` 等前缀已按当前配置自动添加。
     *
     * @param[path] 目标路径。
     */
    private fun buildURL(path: String): Url =
        URLBuilder(
            protocol = if (requireRunningConfig().enableTLS) URLProtocol.HTTPS else URLProtocol.HTTP,
            host = host,
            port = port
        )
            .apply { path(path) }
            .build()

    /**
     * 建立 WebSocket 连接。
     *
     * @param[path] 目标路径。
     * @param[block] 连接处理回调方法。
     */
    private suspend fun openWebSocket(
        path: String,
        block: suspend DefaultClientWebSocketSession.() -> Unit
    ) {
        if (requireRunningConfig().enableTLS) {
            requireClient().wss(
                host = host,
                port = port,
                path = path,
                block = block
            )
        } else {
            requireClient().webSocket(
                host = host,
                port = port,
                path = path,
                block = block
            )
        }
    }

    /**
     * 发送文件传输信令。
     *
     * @param[event] 文件传输信令。
     */
    private suspend inline fun <reified T> DefaultClientWebSocketSession.sendEvent(event: FileTransferEvent<T>) {
        val text = jsonTool.encodeToString(
            FileTransferEvent.serializer(serializer<T>()),
            event
        )
        send(Frame.Text(text))
    }

    /**
     * 解析文件传输信令。
     *
     * `data` 保持为 [JsonElement] 类型，调用方根据 `type` 说明可转换为具体类型。
     *
     * @param[text] JSON 文本。
     * @return 文件传输信令。
     */
    private fun receiveEvent(text: String): FileTransferEvent<JsonElement> {
        return jsonTool.decodeFromString(
            FileTransferEvent.serializer(JsonElement.serializer()),
            text
        )
    }
}
