package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver
import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.ADBRemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.model.toRemoteState
import org.slf4j.LoggerFactory

/**
 * ADB 代理服务器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBProxyServer(

    /**
     * 监听地址。
     *
     * 默认值为 `0.0.0.0` ，在 Ktor 中该值等效于 `*` 通配符，将会监听所有 IPv4 与 IPv6 地址。
     */
    private val host: String = "0.0.0.0",

    /**
     * 监听端口。
     *
     * 默认值为 [Constant.PORT_SERVER_DEFAULT]。
     */
    private val port: Int = Constant.PORT_SERVER_DEFAULT,
) {

    private val logger = LoggerFactory.getLogger(ADBProxyServer::class.java)

    private var server: EmbeddedServer<*, *>? = null

    /**
     * 全局 JSON 工具实例。
     */
    private val jsonTool: Json = Json {
        // 解码时忽略未知字段
        ignoreUnknownKeys = true
        // 编码时对于未指明的字段填入默认值
        encodeDefaults = true
    }


    @Synchronized
    fun start() {
        if (server != null) {
            logger.warn("This server is already running, ignored.")
            return
        }

        if (!ADBController.isInitialized()) {
            ADBController.init()
        }
        ADBObserver.init()

        server = embeddedServer(CIO, host = host, port = port) {
            configure()
        }.start(wait = false)

        logger.info("Server started on [{}:{}].", host, port)
    }

    @Synchronized
    fun stop() {
        server?.stop(500, 2000)
        server = null
        ADBObserver.terminate()
    }


    private fun Application.configure() {
        install(WebSockets)

        routing {
            get(Constant.PATH_VERSION) {
                call.respondText(Constant.PROTOCOL_VERSION, ContentType.Text.Plain)
            }

            post(Constant.PATH_ADB_DEVICE_LIST) {
                val result = onFetchDeviceList()
                call.respondText(result, ContentType.Application.Json)
            }

            webSocket(Constant.PATH_ADB_SHELL) {
                val sn = call.request.queryParameters[Constant.PARAM_SN]
                val available = ADBController.getDevices().any { it.getSN() == sn && it.isOnline() }
                if (sn.isNullOrBlank() || !available) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device not available: $sn"))
                    return@webSocket
                }

                val term = call.request.queryParameters[Constant.PARAM_TERM]
                    ?.takeIf { it.matches(Regex("[A-Za-z0-9._-]{1,64}")) }
                    ?: "xterm-256color"
                val params = call.request.queryParameters
                val cols = params[Constant.PARAM_COLS]?.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(1000) ?: 120
                val rows = params[Constant.PARAM_ROWS]?.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(1000) ?: 30
                ShellBridge.run(this, sn, term, cols, rows)
            }

            webSocket(Constant.PATH_ADB_EVENTS) {
                ADBObserver.devicesState.collect {
                    val frame = Frame.Text(onFetchDeviceList())
                    send(frame)
                }
            }
        }
    }

    private fun onFetchDeviceList(): String {
        val list = ADBController.getDevices()
            .map { device ->
                ADBRemoteDevice(
                    device.getSN(),
                    device.getState().toRemoteState()
                )
            }

        return jsonTool.encodeToString(
            ListSerializer(ADBRemoteDevice.serializer()),
            list
        )
    }
}
