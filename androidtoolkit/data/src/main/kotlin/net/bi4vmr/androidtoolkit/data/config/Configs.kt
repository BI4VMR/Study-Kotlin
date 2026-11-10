package net.bi4vmr.androidtoolkit.data.config

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 配置项。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Configs {

    val COMMON_DARK_MODE = intPreferencesKey("common_dark_mode")

    val COMMON_NODE_NAME = stringPreferencesKey("common_node_name")

    val ADB_EXEC_MODE = intPreferencesKey("adb_exec_mode")

    val ADB_EXEC_PATH_USER = stringPreferencesKey("adb_exec_path_user")

    val PROXY_START_ON_INIT = booleanPreferencesKey("proxy_start_on_init")

    val PROXY_INTERFACE = stringPreferencesKey("proxy_interface")

    val PROXY_PORT = intPreferencesKey("proxy_port")

    val PROXY_TLS = booleanPreferencesKey("proxy_tls")
}
