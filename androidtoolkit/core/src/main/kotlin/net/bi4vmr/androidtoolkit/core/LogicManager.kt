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
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
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
    suspend fun addRemoteExecutor(host: String, port: Int = Constant.PORT_SERVER_DEFAULT) {
        val client = ADBProxyClient(host, port)

        val serverVersion: String
        try {
            serverVersion = client.getServerVersion()
        } catch (e: Exception) {
            client.release()
            throw e
        }

        if (serverVersion != Constant.PROTOCOL_VERSION) {
            client.release()
            throw IllegalStateException("Server version is [$serverVersion], mismatch with [${Constant.PROTOCOL_VERSION}].")
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
            adbProxyServer.setInterface(address, port)
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

    fun setProxyPort(port: Int) {
        taskScope.launch {
            configManager.setProxyPort(port)
        }
    }
}
