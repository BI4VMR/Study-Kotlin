@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.core

import io.ktor.network.tls.certificates.generateCertificate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.mcp.McpService
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.LocalExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.data.config.ConfigManager
import net.bi4vmr.androidtoolkit.data.config.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.data.file.FileManager
import net.bi4vmr.androidtoolkit.data.sql.MainDB
import net.bi4vmr.androidtoolkit.data.sql.dao.PairedPeerDAO
import net.bi4vmr.androidtoolkit.data.sql.dao.RemoteExecutorDAO
import net.bi4vmr.androidtoolkit.data.sql.model.RemoteExecutorRecord
import net.bi4vmr.tool.kotlin.external.adb.cast.util.ScrcpyServerHelper
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBPairClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ShellTransferHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairRequestListener
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairedPeerStore
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairState
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PeerType
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairedPeer
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import org.slf4j.LoggerFactory
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
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(LogicManager::class.java)

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

    /**
     * 已配对对端数据库。
     */
    private val pairedPeerDAO: PairedPeerDAO by lazy {
        initDB()
        MainDB.pairedPeerDAO
    }

    private val remoteExecutorDAO: RemoteExecutorDAO by lazy {
        initDB()
        MainDB.remoteExecutorDAO
    }

    @Volatile
    private var dbInitialized: Boolean = false

    /**
     * 初始化数据库。UI 可能先于 init 协程访问，此处确保目录存在且只初始化一次。
     */
    @Synchronized
    private fun initDB() {
        if (dbInitialized) return
        val dbDir = fileManager.getDBDir().apply { mkdirs() }
        MainDB.init(dbDir.absolutePath)
        dbInitialized = true
    }

    /**
     * Flow : 服务端已批准的客户端，供设置页面展示。
     */
    val pairedClientsFlow: Flow<List<PairedPeer>> by lazy {
        pairedPeerDAO.observe(PeerType.CLIENT)
    }

    /**
     * 已配对客户端存储，由配对模块在批准后写入、在鉴权时读取。
     */
    private val pairedClientStore = object : PairedPeerStore {

        override fun onList(): List<PairedPeer> = pairedPeerDAO.list(PeerType.CLIENT)

        override fun onSave(peer: PairedPeer) = pairedPeerDAO.save(peer)

        override fun onDelete(fingerprint: String) = pairedPeerDAO.delete(fingerprint)
    }

    /**
     * 删除已配对的客户端，该客户端需要重新配对才能访问转发器。
     */
    fun revokePairedClient(fingerprint: String) {
        taskScope.launch {
            pairedClientStore.onDelete(fingerprint)
        }
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

    val nodeNameFlow: Flow<String> = configManager.nodeNameFlow

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

            LocalExecutor.init()

            /* 设置 Scrcpy 服务端文件路径 */
            val serverFile = fileManager.getInternalScrcpyServerFile()
            ScrcpyServerHelper.setServerFile(serverFile)

            /* 设置终端桥接程序文件路径 */
            ShellTransferHelper.setTransferFile(fileManager.getInternalShellTransferFile())

            /* 恢复上次保存的远程执行器 */
            restoreRemoteExecutors()

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
        if (secure) {
            /* 检测配对状态 */
            val pairClient = ADBPairClient(host, port, cSSLConf)
            try {
                val pairState = pairClient.fetchPairInfo()

                /* 未配对，先执行配对流程。 */
                if (pairState == PairState.NOT_PAIRED) {
                    pairClient.nodeName = configManager.getNodeName()
                    pairClient.pair(onPairingCode)
                }
            } finally {
                pairClient.release()
            }
        }

        val client = ADBProxyClient(host, port)
            .apply {
                setSecure(secure)
                setSSLConfig(cSSLConf)
                init()
            }

        // 版本验证
        val serverInfo = client.getServerInfo()
        if (serverInfo.version != Constants.PROTOCOL_VERSION) {
            throw IllegalStateException("Server version is [${serverInfo.version}], mismatch with [${Constants.PROTOCOL_VERSION}]!")
        }

        connectRemoteExecutor(host, port)

        // 连接建立后持久化，下次启动时自动加载。
        remoteExecutorDAO.save(
            RemoteExecutorRecord(
                host = host,
                port = port,
                secure = secure,
                addedTimestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * 建立远程执行器连接并注册到执行器列表，同一地址已存在时忽略。
     */
    private fun connectRemoteExecutor(host: String, port: Int) {
        val exists = _executorsFlow.value.any { it is RemoteExecutor && it.host == host && it.port == port }
        if (exists) return

        val client = ADBProxyClient(host, port)
            .apply {
                setSecure(true)
                setSSLConfig(cSSLConf)
            }
        val executor = RemoteExecutor(host, port, client).apply { init() }
        _executorsFlow.update { it + executor }
    }

    /**
     * 加载数据库中保存的远程执行器。
     *
     * 这些服务端此前已完成配对，因此直接建立连接；单个连接失败不影响其他记录。
     */
    private fun restoreRemoteExecutors() {
        remoteExecutorDAO.list().forEach { record ->
            runCatching { connectRemoteExecutor(record.host, record.port) }
                .onFailure { println("LogicManager: failed to restore remote executor [${record.host}:${record.port}]: $it") }
        }
    }

    /**
     * 移除远程执行器，并删除数据库记录。
     */
    fun removeRemoteExecutor(executor: RemoteExecutor) {
        _executorsFlow.update { list -> list.filterNot { it === executor } }
        if (_selectedExecutorFlow.value === executor) {
            _selectedExecutorFlow.value = LocalExecutor
        }
        executor.release()
        taskScope.launch {
            remoteExecutorDAO.delete(executor.host, executor.port)
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

            adbProxyServer.address = address
            adbProxyServer.port = port
            adbProxyServer.pairRequestListener = pairRequestListener
            adbProxyServer.pairedPeerStore = pairedClientStore
            adbProxyServer.start()
        }
    }

    fun stopProxyServer() = adbProxyServer.stop()


    /* ----- 配置项 ----- */

    fun setNodeName(name: String) {
        taskScope.launch {
            configManager.setNodeName(name)
        }
    }

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
