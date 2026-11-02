@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.core

import io.ktor.network.tls.certificates.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.bi4vmr.androidtoolkit.core.mcp.McpService
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.LocalExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.data.config.ConfigManager
import net.bi4vmr.androidtoolkit.data.config.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.data.file.FileManager
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.cast.util.ScrcpyServerHelper
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBPairClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairRequestListener
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairedPeerStore
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairedPeer
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import java.io.File
import java.net.ConnectException

/**
 * 业务逻辑入口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object LogicManager {

    /**
     * 任务作用域。
     */
    private val taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 文件管理。
     */
    private val fileManager: FileManager = FileManager

    /**
     * 配置管理。
     */
    private val configManager: ConfigManager = ConfigManager(FileManager.getConfigFile(), taskScope)

    /**
     * ADB 转发器。
     */
    val adbProxyServer: ADBProxyServer by lazy { ADBProxyServer() }

    val cSSLConf: SSLConfig by lazy {
        // TODO
        val file = File(fileManager.getUserDataDir(), "client.jks")
        if (!file.exists()) {
            generateCertificate(
                file = file,
                algorithm = "SHA256withRSA",
                keyAlias = "key1",
                keyPassword = "1",
                jksPassword = "1",
                keySizeInBits = 2048
            )
        }
        val conf = SSLConfig(
            keyStoreFile = file,
            keyStorePassword = "1",
            keyAlias = "key1",
            keyPassword = "1"
        )
        conf
    }


    private val _pendingPairingsFlow: MutableStateFlow<List<PairRequest>> = MutableStateFlow(emptyList())

    /**
     * Flow : 待处理的配对申请，界面应按顺序提示用户批准或拒绝。
     *
     * 多个客户端可以同时申请配对，界面一次展示一个，处理完毕后自动展示下一个。
     */
    val pendingPairingsFlow: StateFlow<List<PairRequest>> = _pendingPairingsFlow

    /**
     * 配对请求监听器。
     *
     * 客户端发起确认请求后加入待处理列表，配对结束（用户操作、客户端取消、会话超时或服务端终止）
     * 后自动移除，避免界面残留已经失效的申请。
     */
    private val pairRequestListener = object : PairRequestListener {

        override fun onRequest(request: PairRequest) {
            _pendingPairingsFlow.update { it + request }
        }

        override fun onResult(request: PairRequest, result: PairResult) {
            _pendingPairingsFlow.update { list -> list.filterNot { it === request } }
        }
    }

    private val _pairedClientsFlow: MutableStateFlow<List<PairedPeer>> = MutableStateFlow(emptyList())

    /**
     * Flow : 已配对的客户端 设置页面。
     */
    val pairedClientsFlow: StateFlow<List<PairedPeer>> = _pairedClientsFlow

    /**
     * 已配对客户端存储，变更时同步写入配置文件。
     */
    private val pairedClientStore = object : PairedPeerStore {

        override fun onList(): List<PairedPeer> {
            println("onList")
            return emptyList()
        }

        override fun onSave(peer: PairedPeer) {
            // _pairedClientsFlow.update { old -> old.filterNot { it.id == client.id } + client }
            println("onSave -> $peer")
        }

        override fun onDelete(fingerprint: String) {
            TODO("Not yet implemented")
        }
    }

    // private fun persistPairedClients() {
    //     val entries = _pairedClientsFlow.value
    //         .map { "it.id|${it.pairedAt}|it.tokenHash|${it.name}" }
    //         .toSet()
    //     taskScope.launch { configManager.setPairedClients(entries) }
    // }


    /**
     * Flow : 执行器列表。
     */
    private val _executorsFlow: MutableStateFlow<List<ADBExecutor>> =
        // 本地执行器总是可用的，将其固定放置在列表首位。
        MutableStateFlow(listOf(LocalExecutor))

    /**
     * Flow : 执行器列表。
     */
    val executorsFlow: StateFlow<List<ADBExecutor>> = _executorsFlow

    /**
     * Flow : 当前选中的执行器。
     *
     * 没有远程设备时默认使用本地 ADB 进程。
     */
    private val _selectedExecutorFlow: MutableStateFlow<ADBExecutor> = MutableStateFlow(LocalExecutor)

    /**
     * Flow : 当前选中的执行器。
     *
     * 没有远程设备时默认使用本地 ADB 进程。
     */
    val selectedExecutorFlow: StateFlow<ADBExecutor> = _selectedExecutorFlow

    /**
     * Flow : ADB 设备列表。
     *
     * 跟随当前选中的执行器，若无选中项则为空列表。
     */
    val devicesFlow: StateFlow<List<ADBDevice>> =
        selectedExecutorFlow
            .flatMapLatest { executor ->
                executor.getDevicesFlow()
            }
            .stateIn(
                scope = taskScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Flow : 当前选中的 ADB 设备。
     */
    private val _selectedDeviceFlow: MutableStateFlow<ADBDevice?> = MutableStateFlow(null)

    /**
     * Flow : 当前选中的 ADB 设备。
     */
    val selectedDeviceFlow: StateFlow<ADBDevice?> = _selectedDeviceFlow

    /**
     * 用户上次主动选择的设备序列号。
     */
    @Volatile
    private var preferredSN: String? = null


    /* ----- 配置项 ----- */

    val adbExecFileModeFlow: Flow<ADBExecFileMode> = configManager.adbExecFileModeFlow

    val proxyStartOnInitFlow: Flow<Boolean> = configManager.proxyStartOnInitFlow

    val proxyInterfaceFlow: Flow<String> = configManager.proxyInterfaceFlow

    val proxyPortFlow: Flow<Int> = configManager.proxyPortFlow

    val proxyEncryptFlow: Flow<Boolean> = configManager.proxyEncryptFlow

    val proxyTrustAllFlow: Flow<Boolean> = configManager.proxyTrustAllFlow


    /* ----- 生命周期 ----- */

    init {
        /* 初始化 */
        taskScope.launch {
            fileManager.init()

            val adbExecMode = configManager.getADBExecFileMode()
            when (adbExecMode) {
                ADBExecFileMode.INTERNAL -> {
                    val adbFile = fileManager.getInternalADBExecFile()
                    if (adbFile.exists() && !adbFile.canExecute()) {
                        adbFile.setExecutable(true, false)
                    }
                    ADBExecutableHelper.setExecutableFile(adbFile)
                }

                ADBExecFileMode.AUTO -> {
                    println("LogicManager init: detected adb exec file = ${ADBExecutableHelper.detectExecutable()}")
                }

                ADBExecFileMode.USER -> {
                    val user = configManager.getADBExecFileOfUser()
                    println("LogicManager init: user adb exec file = $user")
                }
            }

            ADBController.init()

            /* 设置 Scrcpy 服务端文件路径 */
            val serverFile = fileManager.getInternalScrcpyServerFile()
            ScrcpyServerHelper.setServerFile(serverFile)

            /* 启动 MCP 服务，环境变量 ANDROIDTOOLKIT_MCP_PORT=0 可禁用 */
            val mcpPort = System.getenv("ANDROIDTOOLKIT_MCP_PORT")?.toIntOrNull() ?: McpService.PORT_DEFAULT
            if (mcpPort > 0) {
                runCatching { McpService.start(mcpPort) }
                    .onFailure { println("LogicManager init: failed to start MCP service: $it") }
            }

            val proxyStartOnInit = configManager.proxyStartOnInitFlow.first()
            if (proxyStartOnInit) {
                startProxyServer()
            }
        }

        /* 设备列表变化时，优先恢复用户上次选择的设备，找不到则选中第一个 */
        taskScope.launch {
            devicesFlow.collect { devices ->
                // 仅修改选中状态，不改变用户偏好的序列号。
                _selectedDeviceFlow.value = devices.firstOrNull { it.getSN() == preferredSN } ?: devices.firstOrNull()
            }
        }
    }

    fun release() {
        // 释放所有远程执行器
        _executorsFlow.value.forEach { it.release() }
    }


    fun selectExecutor(executor: ADBExecutor) {
        _selectedExecutorFlow.value = executor
    }

    fun selectDevice(device: ADBDevice?) {
        if (device != null) {
            preferredSN = device.getSN()
        }
        _selectedDeviceFlow.value = device
    }

    /**
     * 添加远程执行器。
     *
     * @return `true` 表示添加成功， `false` 表示连接失败或已存在。
     * @throws[ConnectException] 服务端协议版本与客户端不匹配。
     * @throws[IllegalStateException] 服务端协议版本与客户端不匹配。
     */
    suspend fun addRemoteExecutor(
        host: String,
        port: Int = Constants.PORT_SERVER_DEFAULT,
        secure: Boolean = true,
        onPairingCode: (String) -> Unit = {}
    ) {
        /* 读取该服务端已保存的配对信息：host:port|fingerprint|token */
        // val serverKey = "$host:$port"
        // val saved = configManager.getPairedServers()
        //     .map { it.split("|", limit = 3) }
        //     .firstOrNull { it.size == 3 && it[0] == serverKey }

        // ADBProxyClient(host,port)
        //     .apply {
        //         setSecure(true)
        //         setSSLConfig(cSSLConf)
        //         init()
        //         delay(1.seconds)
        //     }
        //     .getServerInfo()
        //     .let {
        //         println(it)
        //     }
        // return
        val pairClient = ADBPairClient(host, port, cSSLConf)

        try {
            // TODO 版本验证
            // val serverVersion = pairClient.getServerVersion()
            // if (serverVersion != Constant.PROTOCOL_VERSION) {
            //     throw IllegalStateException("Server version is [$serverVersion], mismatch with [${Constant.PROTOCOL_VERSION}].")
            // }

            if (secure) {
                // TODO 服务端提供配对状态查询后，先通过 `isPaired()` 判断，避免重复配对。
                // 加密连接需要配对：申请配对并等待服务端用户核对配对码后批准。
                pairClient.pair(onPairingCode)
            }

            // TODO 配对成功后使用 `ADBProxyClient` 建立连接，并注册到执行器列表。

            val client = ADBProxyClient(host,port)
                .apply {
                    setSecure(true)
                    setSSLConfig(cSSLConf)
                    init()
                }
            _executorsFlow.update { it + RemoteExecutor(host, port, client) }

            // 5 秒内未返回就绪，认为连接失败。
            // val ok = withTimeoutOrNull(5.seconds) {
            //     client.serviceStateFlow.first { it == ServiceState.READY }
            // } != null
            //
            // if (ok) {
            //     _executorsFlow.update { it + RemoteExecutor(host, port, client) }
            // } else {
            //     client.release()
            //     throw ConnectException("Failed to connect with remote executor [$host:$port].")
            // }
        } finally {
            // 配对客户端仅供配对使用，配对结束（成功、失败或取消）后立即释放。
            pairClient.release()
        }
    }

    fun startCast() {
        val exec = _selectedExecutorFlow.value
        when (exec) {
            is LocalExecutor -> {
                // ADBCastManager.start()
                // exec.startCast()
            }

            is RemoteExecutor -> {
                taskScope.launch {
                    // ADBProxyClient().startCast()
                }
            }
        }
    }

    fun startProxyServer() {
        taskScope.launch {
            // TODO TEST KEY
            val file = File(fileManager.getUserDataDir(), "server.jks")
            if (!file.exists()) {
                generateCertificate(
                    file = file,
                    algorithm = "SHA256withRSA",
                    keyAlias = "Key1",
                    keyPassword = "1",
                    jksPassword = "1",
                    keySizeInBits = 2048
                )
            }
            val conf = SSLConfig(
                keyStoreFile = file,
                keyStorePassword = "1",
                keyAlias = "Key1",
                keyPassword = "1"
            )
            adbProxyServer.sslConfig = conf


            val address = configManager.proxyInterfaceFlow.first()
            val port = configManager.proxyPortFlow.first()

            if (_pairedClientsFlow.value.isEmpty()) {
                _pairedClientsFlow.value = configManager.getPairedClients().mapNotNull { entry ->
                    val parts = entry.split("|", limit = 4)
                    val pairedAt = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                    if (parts.size < 4) return@mapNotNull null
                    // PairedClient(id = parts[0], name = parts[3], tokenHash = parts[2], pairedAt = pairedAt)
                    null
                }
            }

            adbProxyServer.address = address
            adbProxyServer.port = port
            adbProxyServer.pairRequestListener = pairRequestListener
            adbProxyServer.pairedPeerStore = pairedClientStore
            adbProxyServer.start()
        }
    }

    fun stopProxyServer() = adbProxyServer.stop()


    /* ----- 配置项 ----- */

    fun setADBExecMode(mode: ADBExecFileMode) {
        taskScope.launch {
            configManager.setADBExecMode(mode)
        }
    }

    fun setProxyStartOnInit(enabled: Boolean) {
        taskScope.launch {
            configManager.setProxyStartOnInit(enabled)
        }
    }

    fun setProxyInterface(address: String) {
        taskScope.launch {
            configManager.setProxyInterface(address)
        }
    }

    /**
     * 设置是否信任所有连接（仅用于调试），即时生效。
     */
    /**
     * 设置是否启用加密与配对，需要重启转发器后生效。
     */
    fun setProxyEncrypt(enabled: Boolean) {
        taskScope.launch {
            configManager.setProxyEncrypt(enabled)
        }
    }

    fun setProxyTrustAll(enabled: Boolean) {
        // proxySecurity?.trustAllClients = enabled
        taskScope.launch {
            configManager.setProxyTrustAll(enabled)
        }
    }

    fun setProxyPort(port: Int) {
        taskScope.launch {
            configManager.setProxyPort(port)
        }
    }
}
