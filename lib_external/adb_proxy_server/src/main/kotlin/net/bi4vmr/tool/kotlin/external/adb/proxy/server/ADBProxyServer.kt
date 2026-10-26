package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ServerReady
import io.ktor.server.application.install
import io.ktor.server.netty.Netty
import io.ktor.server.engine.EmbeddedServer
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
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
import org.slf4j.LoggerFactory
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.ADBProxySecurity
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.installSecurity
import java.security.KeyStore
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
     * 安全配置，启动前必须通过 [setSecurity] 设置。
     */
    @Volatile
    private var security: ADBProxySecurity? = null

    private val _fingerprintFlow: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Flow : 服务端证书的 SHA-256 指纹（十六进制），启动后可用，供客户端核对。
     */
    val fingerprintFlow: StateFlow<String?> = _fingerprintFlow

    /**
     * 全局 JSON 工具。
     */
    private val jsonTool: Json = Json {
        // 解码时忽略未知字段
        ignoreUnknownKeys = true
        // 编码时对于未指明的字段填入默认值
        encodeDefaults = true
    }


    private val _stateFlow: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.INIT)

    val stateFlow: StateFlow<ServerState> = _stateFlow

    private val _errorFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    val errorFlow: StateFlow<Exception?> = _errorFlow


    @Synchronized
    fun start() {
        if (stateFlow.value != ServerState.INIT) {
            logger.warn("This server is already running, ignored.")
            return
        }

        val sec = security
        if (sec == null) {
            logger.error("Security config is required, call setSecurity() before start().")
            _errorFlow.value = IllegalStateException("Security config is not set.")
            return
        }

        _errorFlow.value = null
        _stateFlow.value = ServerState.STARTING

        if (ADBController.getServiceState() == ServiceState.IDLE) {
            ADBController.init()
        }

        try {
            val keyStore = loadKeyStore(sec)
            _fingerprintFlow.value = ADBProxySecurity.fingerprintOf(keyStore).also { sec.fingerprint = it }

            // 仅提供 TLS 端口，不开放明文连接（Ktor CIO 服务端不支持 HTTPS，故使用 Netty）。
            server = embeddedServer(Netty, configure = {
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = ADBProxySecurity.KEY_ALIAS,
                    keyStorePassword = { ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray() },
                    privateKeyPassword = { ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray() }
                ) {
                    host = address
                    port = this@ADBProxyServer.port
                }
            }) {
                // 监听服务器就绪状态
                monitor.subscribe(ServerReady) { _stateFlow.value = ServerState.RUNNING }

                configure(sec)
            }.start(wait = false)
        } catch (e: Exception) {
            // TODO
            logger.error("Failed to start server on [{}:{}].", address, port, e)
            println("e.message: ${e.message}")
            println("e.type: ${e}")

            _errorFlow.value = e
            _stateFlow.value = ServerState.INIT
        }
    }

    @Synchronized
    fun stop() {
        if (stateFlow.value != ServerState.RUNNING) {
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
     * 设置安全配置。
     *
     * 如果服务端已经启动，该设置不会影响已启动的服务端，若要刷新需先终止后重新启动；
     * 但 [ADBProxySecurity.trustAllClients] 为即时生效。
     */
    fun setSecurity(security: ADBProxySecurity) {
        this.security = security
    }

    private fun loadKeyStore(sec: ADBProxySecurity): KeyStore {
        val file = sec.keyStoreFile
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            generateCertificate(
                file = file,
                algorithm = "SHA256withRSA",
                keyAlias = ADBProxySecurity.KEY_ALIAS,
                keyPassword = ADBProxySecurity.KEYSTORE_PASSWORD,
                jksPassword = ADBProxySecurity.KEYSTORE_PASSWORD,
                keySizeInBits = 2048
            )
        }
        return KeyStore.getInstance(file, ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray())
    }

    private fun Application.configure(security: ADBProxySecurity) {
        // 自动反序列化
        install(ContentNegotiation) { json(jsonTool) }
        // WebSocket 支持
        install(WebSockets)
        // 认证与配对
        installSecurity(security)

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
