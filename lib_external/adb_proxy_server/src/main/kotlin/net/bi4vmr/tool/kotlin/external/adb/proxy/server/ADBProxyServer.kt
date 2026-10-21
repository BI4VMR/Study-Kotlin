package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant
import org.slf4j.LoggerFactory
import java.util.*

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

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBProxyServer::class.java)

    /**
     * Ktor 服务器。
     */
    private var server: EmbeddedServer<*, *>? = null

    /**
     * 全局 JSON 工具。
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

        if (ADBController.getServiceState() == ServiceState.IDLE) {
            ADBController.init()
        }

        try {
            server = embeddedServer(CIO, host = host, port = port) {
                configure()
            }.start(wait = false)
        } catch (e: Exception) {
            // TODO
            logger.error("Failed to start server on [{}:{}].", host, port, e)
            // throw e
            println("e.message: ${e.message}")
            println("e.type: ${e}")
        }

        logger.info("Server started on [{}:{}].", host, port)
    }

    @Synchronized
    fun stop() {
        server?.stop(500, 2000)
        server = null
    }


    private fun Application.configure() {
        // 自动反序列化
        install(ContentNegotiation) { json(jsonTool) }
        // WebSocket 支持
        install(WebSockets)

        routing {
            installInternalRoutes(this)
            installOptionalRoutes(this)
        }
    }

    // 加载核心组件
    private fun installInternalRoutes(routing: Routing) {
        with(ADBCoreRoutes()) { routing.register(jsonTool) }
    }

    // 通过 SPI 机制加载可选组件
    private fun installOptionalRoutes(routing: Routing) {
        ServiceLoader.load(ADBProxyRouteProvider::class.java).forEach { provider ->
            with(provider) { routing.register(jsonTool) }
            logger.info("Optional module [{}] has been registered.", provider.javaClass.name)
        }
    }
}
