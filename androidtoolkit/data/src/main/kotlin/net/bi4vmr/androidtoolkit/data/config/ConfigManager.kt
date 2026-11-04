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
import org.slf4j.LoggerFactory
import java.io.File
import java.net.InetAddress
import java.util.UUID


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

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = taskScope) {
        file
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

    val proxyEncryptFlow: Flow<Boolean> =
        dataStore.data.map {
            it[Configs.Key.PROXY_ENCRYPT] ?: Configs.Value.PROXY_ENCRYPT_DEFAULT
        }

    val proxyTrustAllFlow: Flow<Boolean> =
        dataStore.data.map {
            it[Configs.Key.PROXY_TRUST_ALL] ?: Configs.Value.PROXY_TRUST_ALL_DEFAULT
        }


    init {
        taskScope.launch {
            // 如果节点名称为空，则使用主机名。
            if (getNodeName().isEmpty()) {
                try {
                    val hostname = InetAddress.getLocalHost().hostName
                    setNodeName(hostname)
                } catch (e: Exception) {
                    logger.warn("Get hostname failed! Reason:[${e.message}]")
                    setNodeName(Configs.Value.COMMON_NODE_NAME)
                }
            }
        }
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

    suspend fun setProxyStartOnInit(enabled: Boolean) {
        dataStore.edit { it[Configs.Key.PROXY_START_ON_INIT] = enabled }
    }

    suspend fun setProxyInterface(address: String) {
        dataStore.edit { it[Configs.Key.PROXY_INTERFACE] = address }
    }

    suspend fun setProxyPort(port: Int) {
        dataStore.edit { it[Configs.Key.PROXY_PORT] = port }
    }

    suspend fun setProxyEncrypt(enabled: Boolean) {
        dataStore.edit { it[Configs.Key.PROXY_ENCRYPT] = enabled }
    }

    suspend fun getProxyEncrypt(): Boolean = proxyEncryptFlow.first()

    suspend fun setProxyTrustAll(enabled: Boolean) {
        dataStore.edit { it[Configs.Key.PROXY_TRUST_ALL] = enabled }
    }

    suspend fun getProxyTrustAll(): Boolean = proxyTrustAllFlow.first()


    /* ----- 配对信息 ----- */

    /**
     * 本客户端的唯一标识，首次调用时生成。
     */
    suspend fun getClientID(): String {
        var id = dataStore.data.first()[Configs.Key.CLIENT_ID]
        if (id == null) {
            dataStore.edit {
                id = it[Configs.Key.CLIENT_ID] ?: UUID.randomUUID().toString().also { new ->
                    it[Configs.Key.CLIENT_ID] = new
                }
            }
        }
        return requireNotNull(id)
    }

    /**
     * 服务端侧：已配对的客户端。条目格式： `id|pairedAt|tokenHash|name` ，名称置于末尾以允许包含分隔符。
     */
    val pairedClientsFlow: Flow<Set<String>> =
        dataStore.data.map { it[Configs.Key.PAIRED_CLIENTS] ?: emptySet() }

    suspend fun getPairedClients(): Set<String> = pairedClientsFlow.first()

    suspend fun setPairedClients(entries: Set<String>) {
        dataStore.edit { it[Configs.Key.PAIRED_CLIENTS] = entries }
    }

    /**
     * 客户端侧：已配对的服务端。条目格式： `host:port|fingerprint|token` 。
     */
    suspend fun getPairedServers(): Set<String> =
        dataStore.data.first()[Configs.Key.PAIRED_SERVERS] ?: emptySet()

    suspend fun setPairedServers(entries: Set<String>) {
        dataStore.edit { it[Configs.Key.PAIRED_SERVERS] = entries }
    }
}
