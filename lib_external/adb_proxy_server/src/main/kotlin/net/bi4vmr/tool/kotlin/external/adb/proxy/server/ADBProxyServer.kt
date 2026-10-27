package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ServerReady
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums.ServerState
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.routes.ADBCoreRoutes
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.routes.ADBProxyRouteProvider
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.SSLConfig
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

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
    @Volatile
    private var address: String = "0.0.0.0",

    /**
     * 监听端口。
     *
     * 默认值为 [Constant.PORT_SERVER_DEFAULT]。
     */
    @Volatile
    private var port: Int = Constant.PORT_SERVER_DEFAULT,
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
     * SSL / TLS 配置。
     *
     * 默认为空值，此时不启用加密；配置存在时将使用指定的证书启动服务器。
     */
    private var sslConfig: SSLConfig? = null

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
     * Flow : 服务端状态。
     */
    private val _stateFlow: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.INIT)

    /**
     * Flow : 服务端状态。
     */
    val stateFlow: StateFlow<ServerState> = _stateFlow

    /**
     * Flow : 服务端错误。
     */
    private val _errorFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * Flow : 服务端错误。
     */
    val errorFlow: StateFlow<Exception?> = _errorFlow


    /* ----- 服务状态 ----- */

    @Synchronized
    fun start() {
        if (!canStart()) {
            logger.warn("This server is already running, ignored.")
            return
        }

        /* 重置状态 */
        _errorFlow.value = null
        _stateFlow.value = ServerState.STARTING

        if (ADBController.getServiceState() == ServiceState.IDLE) {
            ADBController.init()
        }

        try {
            server = embeddedServer(Netty, configure = {
                val ssl = sslConfig
                if (ssl != null) {
                    sslConnector(
                        keyStore = ssl.getKeyStore(),
                        keyStorePassword = { ssl.keyStorePassword.toCharArray() },
                        keyAlias = ssl.keyAlias,
                        privateKeyPassword = { ssl.keyPassword.toCharArray() }
                    ) {
                        host = address
                        port = this@ADBProxyServer.port
                    }
                } else {
                    connector {
                        host = address
                        port = this@ADBProxyServer.port
                    }
                }
            }) {
                // 监听服务器就绪状态
                monitor.subscribe(ServerReady) { _stateFlow.value = ServerState.RUNNING }

                // 注册路由与配置插件
                configure()
            }.start(wait = false)
        } catch (e: Exception) {
            // TODO
            logger.error("Failed to start server on [{}:{}].", address, port, e)
            println("e.message: ${e.message}")
            println("e.type: ${e}")

            _errorFlow.value = e
            _stateFlow.value = ServerState.ERROR
        }
    }

    @Synchronized
    fun stop() {
        if (!canStop()) {
            logger.warn("This server is not running, ignored.")
            return
        }

        _stateFlow.value = ServerState.STOPPING

        server?.stop(500, 2000)
        server = null

        _errorFlow.value = null
        _stateFlow.value = ServerState.INIT
    }

    /**
     * 设置监听地址和端口。
     *
     * 如果服务端已经启动，该设置不会影响已启动的服务端，若要刷新需先终止后重新启动。
     *
     * @param[address] 监听地址。
     * @param[port] 监听端口。
     */
    fun setInterface(address: String, port: Int) {
        this.address = address
        this.port = port
    }

    /**
     * 设置 SSL 配置。
     *
     * 传入空值表示不启用加密，传入非空的值将使用该证书启用 SSL / TLS 加密。
     *
     * 如果服务端已经启动，该设置不会影响已启动的服务端，若要刷新需先终止后重新启动。
     *
     * @param[config] SSL 配置。
     */
    fun setSSLConfig(config: SSLConfig?) {
        sslConfig = config
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

    private fun canStart(): Boolean =
        (stateFlow.value == ServerState.INIT) || (stateFlow.value == ServerState.ERROR)

    private fun canStop(): Boolean = (stateFlow.value == ServerState.RUNNING)
}
