@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.core

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
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.ADBProxySecurity
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.PairedClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.PairedClientStore
import java.net.InetAddress
import java.net.ConnectException
import kotlin.time.Duration.Companion.seconds

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

    /**
     * 待用户处理的配对申请。
     */
    class PendingPairing(
        val request: PairRequest,
        val code: String,
        internal val result: CompletableDeferred<Boolean>
    ) {
        fun respond(approve: Boolean) {
            result.complete(approve)
        }
    }

    private val _pendingPairingFlow: MutableStateFlow<PendingPairing?> = MutableStateFlow(null)

    /**
     * Flow : 待处理的配对申请，界面应提示用户批准或拒绝。
     */
    val pendingPairingFlow: StateFlow<PendingPairing?> = _pendingPairingFlow

    private val _pairedClientsFlow: MutableStateFlow<List<PairedClient>> = MutableStateFlow(emptyList())

    /**
     * Flow : 已配对的客户端。
     */
    val pairedClientsFlow: StateFlow<List<PairedClient>> = _pairedClientsFlow

    @Volatile
    private var proxySecurity: ADBProxySecurity? = null

    /**
     * 已配对客户端存储，变更时同步写入配置文件。
     */
    private val pairedClientStore = object : PairedClientStore {
        override fun list(): List<PairedClient> = _pairedClientsFlow.value

        override fun save(client: PairedClient) {
            _pairedClientsFlow.update { old -> old.filterNot { it.id == client.id } + client }
            persistPairedClients()
        }

        override fun remove(clientId: String) {
            _pairedClientsFlow.update { old -> old.filterNot { it.id == clientId } }
            persistPairedClients()
        }
    }

    private fun persistPairedClients() {
        val entries = _pairedClientsFlow.value
            .map { "${it.id}|${it.pairedAt}|${it.tokenHash}|${it.name}" }
            .toSet()
        taskScope.launch { configManager.setPairedClients(entries) }
    }


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
        port: Int = Constant.PORT_SERVER_DEFAULT,
        onPairingCode: (String) -> Unit = {}
    ) {
        /* 读取该服务端已保存的配对信息：host:port|fingerprint|token */
        val serverKey = "$host:$port"
        val saved = configManager.getPairedServers()
            .map { it.split("|", limit = 3) }
            .firstOrNull { it.size == 3 && it[0] == serverKey }

        val localName = runCatching { InetAddress.getLocalHost().hostName }.getOrNull() ?: "AndroidToolkit"
        val client = ADBProxyClient(
            host = host,
            port = port,
            token = saved?.get(2)?.takeIf { it.isNotEmpty() },
            pinnedFingerprint = saved?.get(1)?.takeIf { it.isNotEmpty() },
            clientId = configManager.getClientID(),
            clientName = localName
        )

        try {
            val serverVersion = client.getServerVersion()
            if (serverVersion != Constant.PROTOCOL_VERSION) {
                throw IllegalStateException("Server version is [$serverVersion], mismatch with [${Constant.PROTOCOL_VERSION}].")
            }

            // 未配对或配对已被服务端撤销时，申请配对并等待服务端用户批准。
            if (!client.isPaired()) {
                client.pair(onPairingCode)
            }

            val fingerprint = client.observedFingerprint.orEmpty()
            val token = client.currentToken.orEmpty()
            val entries = configManager.getPairedServers()
                .filterNot { it.startsWith("$serverKey|") } + "$serverKey|$fingerprint|$token"
            configManager.setPairedServers(entries.toSet())
        } catch (e: Exception) {
            client.release()
            throw e
        }

        client.init()
        // 5 秒内未返回就绪，认为连接失败。
        val ok = withTimeoutOrNull(5.seconds) {
            client.serviceStateFlow.first { it == ServiceState.READY }
        } != null

        if (ok) {
            _executorsFlow.update { it + RemoteExecutor(host, port, client) }
        } else {
            client.release()
            throw ConnectException("Failed to connect with remote executor [$host:$port].")
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
            val address = configManager.proxyInterfaceFlow.first()
            val port = configManager.proxyPortFlow.first()

            if (_pairedClientsFlow.value.isEmpty()) {
                _pairedClientsFlow.value = configManager.getPairedClients().mapNotNull { entry ->
                    val parts = entry.split("|", limit = 4)
                    val pairedAt = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                    if (parts.size < 4) return@mapNotNull null
                    PairedClient(id = parts[0], name = parts[3], tokenHash = parts[2], pairedAt = pairedAt)
                }
            }

            val security = ADBProxySecurity(
                keyStoreFile = fileManager.getConfigDir().resolve("server.jks"),
                store = pairedClientStore,
                trustAllClients = configManager.getProxyTrustAll(),
                pairingApprover = { request, code -> askPairingApproval(request, code) }
            )
            proxySecurity = security

            adbProxyServer.setInterface(address, port)
            adbProxyServer.setSecurity(security)
            adbProxyServer.start()
        }
    }

    // 挂起等待界面处理配对申请，界面无响应时由服务端超时机制取消。
    private suspend fun askPairingApproval(request: PairRequest, code: String): Boolean {
        val pending = PendingPairing(request, code, CompletableDeferred())
        _pendingPairingFlow.value = pending
        return try {
            pending.result.await()
        } finally {
            _pendingPairingFlow.compareAndSet(pending, null)
        }
    }

    /**
     * 撤销客户端的配对，其令牌立即失效。
     */
    fun revokePairedClient(clientId: String) = pairedClientStore.remove(clientId)

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
    fun setProxyTrustAll(enabled: Boolean) {
        proxySecurity?.trustAllClients = enabled
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
