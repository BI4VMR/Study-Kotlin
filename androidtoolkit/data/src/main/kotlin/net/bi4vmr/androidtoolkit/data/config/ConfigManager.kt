package net.bi4vmr.androidtoolkit.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * 系统配置管理器。
 *
 * 基于 DataStore Preferences 持久化系统配置。同一个文件只能创建一个实例。
 *
 * @param file 配置文件，扩展名必须为 "preferences_pb"。
 */
class ConfigManager(
    file: File = File(System.getProperty("user.home"), ".AndroidToolkit_Exp/system_config.preferences_pb"),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        file.parentFile?.mkdirs()
        file
    }


    /** 是否开启代理服务（默认关闭）。 */
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


    suspend fun setProxyStartOnInit(enabled: Boolean) {
        dataStore.edit { it[Configs.Key.PROXY_START_ON_INIT] = enabled }
    }

    suspend fun setProxyInterface(address: String) {
        dataStore.edit { it[Configs.Key.PROXY_INTERFACE] = address }
    }

    suspend fun setProxyPort(port: Int) {
        dataStore.edit { it[Configs.Key.PROXY_PORT] = port }
    }
}
