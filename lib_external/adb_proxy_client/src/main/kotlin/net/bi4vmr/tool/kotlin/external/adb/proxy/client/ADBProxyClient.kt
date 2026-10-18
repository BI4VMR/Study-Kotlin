package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.ADBRemoteDevice
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.ServerSocket
import kotlin.time.Duration.Companion.seconds

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
    private val host: String,

    /**
     * 监听端口。
     *
     * 默认值为 [Constant.PORT_SERVER_DEFAULT]。
     */
    private val port: Int = Constant.PORT_SERVER_DEFAULT,
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
        // 自动反序列化
        install(ContentNegotiation) {
            json(jsonTool)
        }
        // WebSocket 支持
        install(WebSockets)
    }

    /**
     * 子任务作用域。
     */
    private val taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO)


    /**
     * Flow : 连接状态。
     */
    private val _connectStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Flow : 连接状态。
     */
    val connectStateFlow: StateFlow<Boolean> = _connectStateFlow

    /**
     * Flow : 远程设备列表。
     */
    private val _devicesFlow: MutableStateFlow<List<ADBRemoteDevice>> = MutableStateFlow(emptyList())

    /**
     * Flow : 远程设备列表。
     */
    val devicesFlow: StateFlow<List<ADBRemoteDevice>> = _devicesFlow


    /* ----- 状态管理 ----- */

    /**
     * 初始化。
     *
     * 与服务器建立连接，连接成功后可监听设备列表变化等事件。
     *
     * 本方法不会阻塞调用线程，连接状态可通过 [connectStateFlow] 获取。
     */
    fun init() {
        connectWebSocket()
    }

    private fun connectWebSocket() {
        taskScope.launch {
            // 协程未被终止时如果出现异常，继续尝试重连。
            while (isActive) {
                try {
                    // WebSocket 请求只能填写路径，不能携带 `ws://` 等前缀。
                    client.webSocket(host = host, port = port, path = Constant.PATH_ADB_EVENTS) {
                        _connectStateFlow.value = true
                        logger.info("Connected to server: $host:$port")

                        // 循环处理接收到的事件帧
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                _devicesFlow.value = jsonTool.decodeFromString(
                                    ListSerializer(ADBRemoteDevice.serializer()),
                                    frame.readText()
                                )
                            } else {
                                throw IllegalArgumentException("Unsupported frame type!")
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    logger.info("Remote connect job cancelled.")
                    throw e
                } catch (e: Exception) {
                    logger.warn("Failed to connect with server, will retry after delay. Reason:[${e.message}]")
                    _connectStateFlow.value = false
                }

                delay(3.seconds)
            }
        }
    }

    fun isReady(): Boolean = _connectStateFlow.value

    fun release() {
        taskScope.cancel()
        client.close()
    }

    /**
     * 获取服务端版本。
     *
     * 可以将服务端版本号与 [Constant.PROTOCOL_VERSION] 比较，防止出现兼容性问题，该策略非强制，上层应用可在初始化时执行。
     *
     * @return 版本号。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun getServerVersion(): String {
        val url = buildURL(Constant.PATH_VERSION)
        return client.get(url).bodyAsText()
    }

    /**
     * 获取设备列表。
     *
     * @return 设备列表。
     * @throws[ConnectException] 建立连接失败。
     */
    suspend fun fetchDeviceList(): List<ADBRemoteDevice> {
        val url = buildURL(Constant.PATH_ADB_DEVICE_LIST)
        return client.post(url).body()
    }


    fun openShell(device: ADBRemoteDevice) {
        taskScope.launch {
            var localServer: ServerSocket? = null
            try {
                client.webSocket(
                    host = host,
                    port = port,
                    path = "${Constant.PATH_ADB_SHELL}?${Constant.PARAM_SN}=${device.sn}"
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


    private fun buildURL(path: String): String = "http://$host:$port$path"
}
