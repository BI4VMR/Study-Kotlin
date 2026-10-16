package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.ADBRemoteDevice
import org.slf4j.LoggerFactory
import java.net.ConnectException
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

    private val logger = LoggerFactory.getLogger(ADBProxyClient::class.java)

    /**
     * 全局 JSON 工具实例。
     */
    private val jsonTool: Json = Json {
        // 解码时忽略未知字段
        ignoreUnknownKeys = true
        // 编码时对于未指明的字段填入默认值
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        // 自动反序列化
        install(ContentNegotiation) {
            json(jsonTool)
        }
        // WebSocket 支持
        install(WebSockets)
    }

    private val taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var job: Job? = null

    private val _devicesFlow: MutableStateFlow<List<ADBRemoteDevice>> = MutableStateFlow(emptyList())

    val devicesFlow: StateFlow<List<ADBRemoteDevice>> = _devicesFlow


    /**
     * 在本机开启 Shell 桥接端口。
     *
     * 桥接端口仅监听 `127.0.0.1` ，每个接入的 TCP 连接对应目标设备上的一个交互式 Shell 会话。可使用 `socat` 接入系统原生终端：
     *
     * `sh -c "stty raw -echo; socat - TCP:127.0.0.1:<port>; stty sane"`
     *
     * 终端必须先由 `stty raw -echo` 切换为 raw 模式，不要使用 socat 的 `raw` 选项：socat 1.7.3.x 会将其误用于 TCP 连接，
     * 导致方向键、Tab 等按键显示为 `^[[A` 而无效。
     *
     * @param[sn] 目标设备序列号。
     * @param[localPort] 本地端口， `0` 表示自动分配。
     * @return 桥接对象，通过 [ShellBridge.port] 获取端口，关闭后停止监听。
     */
    @JvmOverloads
    fun openShellBridge(sn: String, localPort: Int = 0): ShellBridge =
        ShellBridge(client, host, port, sn, localPort)

    fun release() {
        client.close()
    }

    /**
     * 获取服务端版本。
     *
     * 可以将服务端版本号与 [Constant.PROTOCOL_VERSION] 比较，防止出现兼容性问题，该策略非强制，上层应用可在初始化时执行。
     *
     * 除此之外，该方法可用于测试服务端是否就绪。
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

    fun startListenDeviceList() {
        job = taskScope.launch {
            // 协程未终止时如果出现异常，继续尝试重连。
            while (isActive) {
                try {
                    // WebSocket 请求只需要路径，主机与端口通过独立参数传入
                    client.webSocket(host = host, port = port, path = Constant.PATH_ADB_EVENTS) {
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
                    logger.info("Remote devices listen job cancelled.")
                    throw e
                } catch (e: Exception) {
                    logger.warn("Failed to connect with server, will retry after delay. Reason:[${e.message}]")
                }

                delay(3.seconds)
            }
        }
    }

    fun stopListenDeviceList() {
        job?.cancel()
        job = null
    }


    private fun buildURL(path: String): String = "http://$host:$port$path"
}
