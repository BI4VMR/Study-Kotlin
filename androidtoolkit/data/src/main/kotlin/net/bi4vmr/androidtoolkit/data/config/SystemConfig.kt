package net.bi4vmr.androidtoolkit.data.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * 系统配置管理器。
 *
 * 基于 DataStore Preferences 持久化系统配置。同一个文件只能创建一个实例。
 *
 * @param file 配置文件，扩展名必须为 "preferences_pb"。
 */
class SystemConfig(
    file: File = File(System.getProperty("user.home"), ".androidtoolkit/system_config.preferences_pb"),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        file.parentFile?.mkdirs()
        file
    }

    /** 是否开启代理服务（默认关闭）。 */
    val proxyEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_PROXY_ENABLED] ?: DEFAULT_PROXY_ENABLED }

    /** 获取“是否开启代理服务”的当前值。 */
    suspend fun isProxyEnabled(): Boolean = proxyEnabled.first()

    /** 设置“是否开启代理服务”。 */
    suspend fun setProxyEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PROXY_ENABLED] = enabled }
    }

    private companion object {
        val KEY_PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        const val DEFAULT_PROXY_ENABLED = false
    }
}
