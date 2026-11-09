@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.core

import io.ktor.network.tls.certificates.generateCertificate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.base.model.ExecutorRecord
import net.bi4vmr.androidtoolkit.core.mcp.McpService
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.LocalExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.data.config.ConfigManager
import net.bi4vmr.androidtoolkit.data.config.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.data.config.enums.DarkMode
import net.bi4vmr.androidtoolkit.data.file.FileManager
import net.bi4vmr.androidtoolkit.data.sql.MainDB
import net.bi4vmr.androidtoolkit.data.sql.dao.ExecutorRecordDAO
import net.bi4vmr.androidtoolkit.data.sql.dao.PairedPeerDAO
import net.bi4vmr.androidtoolkit.data.sql.dao.RemoteDeviceDAO
import net.bi4vmr.androidtoolkit.data.sql.model.RemoteDeviceRecord
import net.bi4vmr.tool.kotlin.external.adb.ADBController
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

    /**
     * SSL 配置。
     */
    val sslConfig: SSLConfig by lazy { getDefaultSSLConfig() }


    // Kotlin Object 中的属性与初始化块按照声明顺序执行，因此必须先声明数据库初始化逻辑再声明相关属性。
    init {
        val dbDir = fileManager.getDBDir().apply { mkdirs() }
        MainDB.init(dbDir.absolutePath)
    }

    /**
     * 数据访问类：远程执行器。
     */
    private val executorRecordDAO: ExecutorRecordDAO by lazy {
        MainDB.executorRecordDAO
    }

    /**
     * 数据访问类：远程设备。
     */
    private val remoteDeviceDAO: RemoteDeviceDAO by lazy {
        MainDB.remoteDeviceDAO
    }

    /**
     * 数据访问类：已认证执行器。
     */
    private val pairedPeerDAO: PairedPeerDAO by lazy {
        MainDB.pairedPeerDAO
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
     *
     * 数据驱动，根据数据库记录生成执行器列表，并跟随数据库刷新。
     */
    val executorsFlow: StateFlow<List<ADBExecutor>> =
        executorRecordDAO.observeAll().flatMapLatest { records ->
            // 本地执行器总是可用的，将其固定放置在列表首位。
            val list: MutableList<ADBExecutor> = mutableListOf(LocalExecutor)
            // 根据存储的记录创建远程执行器
            records.forEach { record ->
                val executor = RemoteExecutor(record.id, record.host, record.port)
                executor.name = record.name
                executor.client.apply {
                    enableTLS = record.enableTLS
                    sslConfig = this@LogicManager.sslConfig
                }

                list.add(executor)
            }

            flowOf(list)
        }.stateIn(
            taskScope,
            SharingStarted.WhileSubscribed(5000),
            listOf(LocalExecutor)
        )

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

    val darkModeFlow: Flow<DarkMode> = configManager.darkModeFlow

    val nodeNameFlow: Flow<String> = configManager.nodeNameFlow

    val adbExecFileModeFlow: Flow<ADBExecFileMode> = configManager.adbExecFileModeFlow

    val proxyStartOnInitFlow: Flow<Boolean> = configManager.proxyStartOnInitFlow

    val proxyInterfaceFlow: Flow<String> = configManager.proxyInterfaceFlow

    val proxyPortFlow: Flow<Int> = configManager.proxyPortFlow

    val proxyTLSFlow: Flow<Boolean> = configManager.proxyTLSFlow


    /* ----- 生命周期 ----- */

    init {
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

    /**
     * 释放资源。
     *
     * 本类不支持重复初始化，通常仅在进程被关闭前调用。
     */
    fun release() {
        taskScope.cancel()
        // 释放所有远程执行器
        executorsFlow.value.forEach { it.release() }
    }


    fun selectExecutor(executor: ADBExecutor) {
        _selectedExecutorFlow.value = executor
    }

    fun getSelectedExecutor(): ADBExecutor = _selectedExecutorFlow.value

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
        enableTLS: Boolean = true,
        onPairingCode: (String) -> Unit = {}
    ) {
        // 如果主机信息已存在，则不进行添加。
        val exist = executorRecordDAO.checkIfExist(host, port)
        if (exist) {
            throw IllegalStateException("Remote executor [$host:$port] already exist!")
        }

        if (enableTLS) {
            /* 检测配对状态 */
            val pairClient = ADBPairClient(host, port, sslConfig)
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
                this.enableTLS = enableTLS
                this.sslConfig = this@LogicManager.sslConfig
            }
        client.init()

        // 版本验证
        val serverInfo = client.getServerInfo()
        if (serverInfo.version != Constants.PROTOCOL_VERSION) {
            throw IllegalStateException("Server version is [${serverInfo.version}], mismatch with [${Constants.PROTOCOL_VERSION}]!")
        }

        // 连接成功则记录至数据库
        val record = ExecutorRecord(
            name = serverInfo.name,
            host = host,
            port = port,
            enableTLS = enableTLS
        )
        executorRecordDAO.save(record)
    }

    /**
     * 移除远程执行器，并删除数据库记录。
     */
    fun removeRemoteExecutor(executorID: String) {
        val executor = executorsFlow.value.find { it.getID() == executorID } ?: return

        // 如果被删除的项也是当前选中的项，则把选中状态切换至本地执行器。
        val selectedExecutorID = getSelectedExecutor().getID()
        if (selectedExecutorID == executorID) {
            selectExecutor(LocalExecutor)
        }

        executor.release()

        executorRecordDAO.delete(executor.getID())
    }

    /**
     * 添加远程设备。
     *
     * @param[host] 设备地址。
     * @param[port] 设备端口。
     * @param[save] 是否记住设备。
     * @throws[IllegalArgumentException] 端口号超出范围。
     * @throws[IllegalStateException] 连接失败。错误信息可通过异常消息读取。
     */
    fun addRemoteDevice(host: String, port: Int, save: Boolean) {
        val executor = getSelectedExecutor()

        // 如果主机信息已存在，则不进行添加。
        val exist = remoteDeviceDAO.checkIfExist(host, port, executor.getID())
        if (exist) {
            throw IllegalStateException("Remote device [$host:$port] already exist in [$executor]!")
        }

        ADBController.connectRemoteDevice(host, port)

        // 连接成功后，如果需要保存设备信息，在子线程写入数据库。
        if (save) {
            val record = RemoteDeviceRecord(
                host = host,
                port = port,
                executorID = executor.getID()
            )
            remoteDeviceDAO.replace(record)
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
            adbProxyServer.apply {
                name = configManager.getNodeName()
                address = configManager.getProxyInterface()
                port = configManager.getProxyPort()
                if (configManager.getProxyTLS()) {
                    sslConfig = this@LogicManager.sslConfig
                }
                pairRequestListener = this@LogicManager.pairRequestListener
                pairedPeerStore = this@LogicManager.pairedClientStore
            }

            adbProxyServer.start()
        }
    }

    fun stopProxyServer() = adbProxyServer.stop()


    /* ----- 配置项 ----- */

    fun setDarkMode(mode: DarkMode) {
        taskScope.launch {
            configManager.setDarkMode(mode)
        }
    }

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

    fun setProxyPort(port: Int) {
        taskScope.launch {
            configManager.setProxyPort(port)
        }
    }

    fun setProxyTLS(enable: Boolean) {
        taskScope.launch {
            configManager.setProxyTLS(enable)
        }
    }


    /* ----- 内部方法 ----- */

    /**
     * 获取默认 SSL 证书配置。
     *
     * 如果证书文件不存在，则使用默认配置重新生成。
     *
     * @return 配置。
     */
    private fun getDefaultSSLConfig(): SSLConfig {
        val keyName = "default"
        // 该证书仅使用公钥表明节点身份，不用于验证节点身份，无需密码保护。
        val pwd = ""

        val file = FileManager.getSSLCertFile()
        if (!file.exists()) {
            generateCertificate(
                file = file,
                algorithm = "SHA256withRSA",
                jksPassword = pwd,
                keyAlias = keyName,
                keyPassword = pwd,
                keySizeInBits = 2048
            )
        }

        return SSLConfig(
            keyStoreFile = file,
            keyStorePassword = pwd,
            keyAlias = keyName,
            keyPassword = pwd
        )
    }
}
