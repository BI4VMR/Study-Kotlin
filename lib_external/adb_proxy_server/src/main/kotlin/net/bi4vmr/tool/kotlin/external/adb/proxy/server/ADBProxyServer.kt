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
import io.ktor.server.websocket.WebSockets
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairRequestListener
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairedPeerStore
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums.ServerState
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.module.ADBCoreModule
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.module.ADBProxyModule
import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import javax.net.ssl.KeyManagerFactory

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
    var address: String = "0.0.0.0",

    /**
     * 监听端口。
     *
     * 默认值为 [Constants.PORT_SERVER_DEFAULT]。
     */
    @Volatile
    var port: Int = Constants.PORT_SERVER_DEFAULT,
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBProxyServer::class.java)

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
     * 显示名称。
     *
     * 便于客户端区分每个服务端实例。
     */
    @Volatile
    var name: String = ADBProxyServer::class.java.simpleName

    /**
     * SSL 配置。
     *
     * 默认为空值，此时不启用加密；值不为空则启用 TLS 加密。
     *
     * 修改该配置不会影响已启动的服务端，若要刷新需终止服务端并重新启动。
     */
    @Volatile
    var sslConfig: SSLConfig? = null

    /**
     * 配对模块：已知对等体信息存储。
     *
     * 加载配对模块 `adb_proxy_server_pair` 时，调用者应当通过文件或数据库管理已知对等体信息。
     */
    @Volatile
    var pairedPeerStore: PairedPeerStore? = null

    /**
     * 配对模块：配对请求监听器。
     *
     * 加载配对模块 `adb_proxy_server_pair` 时，每当客户端发起配对请求将会触发该回调，调用者应当向用户展示配对码并等待用户确认。
     *
     * 具有 GUI 的应用程序需要注意切换到主线程再更新界面。
     */
    @Volatile
    var pairRequestListener: PairRequestListener? = null

    /**
     * 运行时配置。
     *
     * 服务启动时复制上述配置变量，以免运行过程中配置变更导致错误。
     */
    private var runningConfig: RunningConfig? = null

    /**
     * Ktor 服务器。
     */
    private var server: EmbeddedServer<*, *>? = null

    /**
     * 已加载的模块列表。
     */
    private val modules: MutableList<ADBProxyModule> = mutableListOf()


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


    /* ----- 服务端配置 ----- */

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

        try {
            // 如果 ADB 服务未启动，则先初始化 ADB 服务。
            if (ADBController.getServiceState() == ServiceState.INIT) {
                ADBController.init()
            }

            val pairedPeerStore = this.pairedPeerStore
            requireNotNull(pairedPeerStore) { "Paired peer store is required!" }

            // 复制关键配置
            val conf = RunningConfig(
                name,
                address,
                port,
                sslConfig,
                pairedPeerStore,
                pairRequestListener
            )
            runningConfig = conf

            server = embeddedServer(Netty, configure = {
                val ssl = conf.sslConfig
                if (ssl != null) {
                    /* 启用 TLS */
                    sslConnector(
                        keyStore = ssl.getKeyStore(),
                        keyStorePassword = { ssl.keyStorePassword.toCharArray() },
                        keyAlias = ssl.keyAlias,
                        privateKeyPassword = { ssl.keyPassword.toCharArray() }
                    ) {
                        host = conf.address
                        port = conf.port
                    }

                    // TODO Ktor 仅在配置 trustStore 时才要求客户端证书，且只信任其中已有的证书，无法满足配对场景。
                    // 此处替换为自定义的 SslHandler ：可选地索取客户端证书并全部接受，由配对模块按指纹鉴权。
                    val sslContext = buildServerSSLContext(ssl)
                    // TODO 启用 HTTP/2 时 Ktor 会在 TLS 握手完成后才调用 channelPipelineConfig ，
                    // 此时替换 SslHandler 将破坏已建立的会话，因此必须关闭 HTTP/2 。
                    enableHttp2 = false
                    channelPipelineConfig = {
                        if (get("ssl") != null) {
                            replace("ssl", "ssl", sslContext.newHandler(channel().alloc()))
                        }
                    }
                } else {
                    /* 禁用 TLS */
                    connector {
                        host = conf.address
                        port = conf.port
                    }
                }
            }) {
                // 监听服务器就绪状态
                monitor.subscribe(ServerReady) { _stateFlow.value = ServerState.RUNNING }

                // 注册路由与配置插件
                configure()
            }.start(wait = false)
        } catch (e: Exception) {
            logger.error(
                "Failed to start server on [{}:{}]! Reason:[{}]",
                runningConfig?.address,
                runningConfig?.port,
                e.message
            )

            _errorFlow.value = e
            _stateFlow.value = ServerState.ERROR
        }
    }

    private fun buildServerSSLContext(ssl: SSLConfig): SslContext {
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(ssl.getKeyStore(), ssl.keyPassword.toCharArray())
        return SslContextBuilder.forServer(keyManagerFactory)
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .clientAuth(ClientAuth.OPTIONAL)
            .build()
    }

    @Synchronized
    fun stop() {
        if (!canStop()) {
            logger.warn("This server is not running, ignored.")
            return
        }

        _stateFlow.value = ServerState.STOPPING

        // 使子模块释放资源
        modules.forEach { runCatching { it.unregister() } }
        modules.clear()

        // TODO ?
        server?.stop(500, 2000)
        server = null

        _errorFlow.value = null
        _stateFlow.value = ServerState.INIT
    }

    fun getState(): ServerState = stateFlow.value

    /**
     * 获取运行时配置。
     *
     * @return 运行时配置。
     */
    fun getRunningConfig(): RunningConfig? = runningConfig


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

    // 是否可以启动服务端
    private fun canStart(): Boolean =
        (getState() == ServerState.INIT) || (getState() == ServerState.ERROR)

    // 是否可以终止服务端
    private fun canStop(): Boolean = (getState() == ServerState.RUNNING)
}
