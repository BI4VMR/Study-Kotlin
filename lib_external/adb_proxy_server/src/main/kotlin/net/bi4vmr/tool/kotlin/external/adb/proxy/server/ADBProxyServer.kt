package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums.ServerState
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.module.ADBCoreModule
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.module.ADBProxyModule
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
    @Volatile
    private var sslConfig: SSLConfig? = null

    /**
     * 已加载的模块列表。
     */
    private val modules: MutableList<ADBProxyModule> = mutableListOf()

    /**
     * 全局 JSON 工具。
     */
    val jsonTool: Json = Json {
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
        modules.clear()
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

        // 使子模块释放资源
        modules.forEach { runCatching { it.unregister() } }
        modules.clear()

        _errorFlow.value = null
        _stateFlow.value = ServerState.INIT
    }

    /**
     * 设置监听地址和端口。
     *
     * 该配置不会影响已启动的服务端，若要刷新需终止服务端并重新启动。
     *
     * @param[address] 监听地址。
     * @param[port] 监听端口。
     */
    fun setInterface(address: String, port: Int) {
        this.address = address
        this.port = port
    }

    fun getSSLConfig(): SSLConfig? = sslConfig

    /**
     * 设置 SSL 配置。
     *
     * 传入空值表示不启用加密，传入非空的值将使用该证书启用 SSL / TLS 加密。
     *
     * 该配置不会影响已启动的服务端，若要刷新需终止服务端并重新启动。
     *
     * @param[config] SSL 配置。
     */
    fun setSSLConfig(config: SSLConfig?) {
        sslConfig = config
    }


    // 配置服务器
    private fun Application.configure() {
        // 自动反序列化
        install(ContentNegotiation) { json(jsonTool) }
        // WebSocket 支持
        install(WebSockets)

        installInternalModules(this)
        installOptionalModules(this)
    }

    // 加载内部组件
    private fun installInternalModules(ktor: Application) {
        val coreModule = ADBCoreModule()
        with(coreModule) { ktor.register(this@ADBProxyServer) }
        modules.add(coreModule)
    }

    // 通过 SPI 机制加载可选组件
    private fun installOptionalModules(ktor: Application) {
        ServiceLoader.load(ADBProxyModule::class.java)
            .forEach { provider ->
                logger.info("Optional module [{}] detected.", provider.javaClass.simpleName)
                with(provider) { ktor.register(this@ADBProxyServer) }
                logger.info("Optional module [{}] has been registered.", provider.javaClass.simpleName)

                modules.add(provider)
            }
    }

    private fun canStart(): Boolean =
        (stateFlow.value == ServerState.INIT) || (stateFlow.value == ServerState.ERROR)

    private fun canStop(): Boolean = (stateFlow.value == ServerState.RUNNING)
}
