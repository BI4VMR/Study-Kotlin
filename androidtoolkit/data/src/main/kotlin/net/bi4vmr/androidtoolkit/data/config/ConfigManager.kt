package net.bi4vmr.androidtoolkit.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.data.config.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.data.config.enums.DarkMode
import net.bi4vmr.tool.java.common.base.net.NetUtil
import org.slf4j.LoggerFactory
import java.io.File


/**
 * 配置管理。
 *
 * 基于 AndroidX DataStore 存储键值对配置项。
 *
 * @param[file] 配置文件。扩展名应为 `preferences_pb` 。
 * @param[taskScope] 任务作用域。
 */
class ConfigManager(

    /**
     * 配置文件。
     */
    file: File,

    /**
     * 任务作用域。
     */
    taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ConfigManager::class.java)

    /**
     * AndroidX DataStore.
     */
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = taskScope) {
        file
    }


    val darkModeFlow: Flow<DarkMode> =
        dataStore.data.map {
            val code = it[Configs.Key.COMMON_DARK_MODE] ?: DarkMode.AUTO.code
            DarkMode.fromCode(code)
        }

    val nodeNameFlow: Flow<String> =
        dataStore.data.map {
            it[Configs.Key.COMMON_NODE_NAME] ?: ""
        }

    val adbExecFileModeFlow: Flow<ADBExecFileMode> =
        dataStore.data.map {
            val code = it[Configs.Key.ADB_EXEC_MODE] ?: Configs.Value.ADB_EXEC_MODE_DEFAULT
            ADBExecFileMode.fromCode(code)
        }

    val adbExecFileOfUser: Flow<File?> =
        dataStore.data.map {
            val path = it[Configs.Key.ADB_EXEC_PATH_USER]
            if (path.isNullOrBlank()) null else File(path)
        }

    val proxyStartOnInitFlow: Flow<Boolean> =
        dataStore.data.map {
            it[Configs.Key.PROXY_START_ON_INIT] ?: Configs.Value.PROXY_START_ON_INIT_DEFAULT
        }

    val proxyInterfaceFlow: Flow<String> =
        dataStore.data.map {
            it[Configs.Key.PROXY_INTERFACE] ?: Configs.Value.PROXY_INTERFACE_DEFAULT
        }

    val proxyPortFlow: Flow<Int> =
        dataStore.data.map {
            it[Configs.Key.PROXY_PORT] ?: Configs.Value.PROXY_PORT_DEFAULT
        }

    val proxyTLSFlow: Flow<Boolean> =
        dataStore.data.map {
            it[Configs.Key.PROXY_TLS] ?: Configs.Value.PROXY_TLS_DEFAULT
        }


    init {
        taskScope.launch {
            // 如果节点名称为空，则使用主机名。
            if (getNodeName().isEmpty()) {
                setNodeName(NetUtil.getHostName())
            }
        }
    }


    suspend fun getDarkMode(): DarkMode = darkModeFlow.first()

    suspend fun setDarkMode(mode: DarkMode) {
        dataStore.edit { it[Configs.Key.COMMON_DARK_MODE] = mode.code }
    }

    suspend fun getNodeName(): String =
        nodeNameFlow.first()

    suspend fun setNodeName(name: String) {
        dataStore.edit { it[Configs.Key.COMMON_NODE_NAME] = name }
    }

    suspend fun getADBExecFileMode(): ADBExecFileMode =
        adbExecFileModeFlow.first()

    suspend fun getADBExecFileOfUser(): File? =
        adbExecFileOfUser.first()

    suspend fun setADBExecMode(mode: ADBExecFileMode) {
        dataStore.edit { it[Configs.Key.ADB_EXEC_MODE] = mode.code }
    }

    suspend fun setADBExecFileOfUser(file: File) {
        dataStore.edit { it[Configs.Key.ADB_EXEC_PATH_USER] = file.absolutePath }
    }

    suspend fun getProxyStartOnInit(): Boolean = proxyStartOnInitFlow.first()

    suspend fun setProxyStartOnInit(enabled: Boolean) {
        dataStore.edit { it[Configs.Key.PROXY_START_ON_INIT] = enabled }
    }

    suspend fun getProxyInterface(): String = proxyInterfaceFlow.first()

    suspend fun setProxyInterface(address: String) {
        dataStore.edit { it[Configs.Key.PROXY_INTERFACE] = address }
    }

    suspend fun getProxyPort(): Int = proxyPortFlow.first()

    suspend fun setProxyPort(port: Int) {
        dataStore.edit { it[Configs.Key.PROXY_PORT] = port }
    }

    suspend fun setProxyTLS(enable: Boolean) {
        dataStore.edit { it[Configs.Key.PROXY_TLS] = enable }
    }

    suspend fun getProxyTLS(): Boolean = proxyTLSFlow.first()
}
