package net.bi4vmr.androidtoolkit.data.config

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import net.bi4vmr.androidtoolkit.data.config.enums.ADBExecFileMode
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant

/**
 * 配置项。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Configs {

    object Key {
        val ADB_EXEC_MODE = intPreferencesKey("adb_exec_mode")

        val ADB_EXEC_PATH_USER = stringPreferencesKey("adb_exec_path_user")

        val PROXY_START_ON_INIT = booleanPreferencesKey("proxy_start_on_init")

        val PROXY_INTERFACE = stringPreferencesKey("proxy_interface")

        val PROXY_PORT = intPreferencesKey("proxy_port")

        val PROXY_TRUST_ALL = booleanPreferencesKey("proxy_trust_all")

        /** 服务端已配对的客户端，条目格式见 [ConfigManager]。 */
        val PAIRED_CLIENTS = stringSetPreferencesKey("paired_clients")

        /** 客户端已配对的服务端，条目格式见 [ConfigManager]。 */
        val PAIRED_SERVERS = stringSetPreferencesKey("paired_servers")

        val CLIENT_ID = stringPreferencesKey("client_id")
    }

    object Value {
        val ADB_EXEC_MODE_DEFAULT: Int = ADBExecFileMode.INTERNAL.code

        const val PROXY_START_ON_INIT_DEFAULT: Boolean = true

        const val PROXY_INTERFACE_DEFAULT: String = "0.0.0.0"

        const val PROXY_PORT_DEFAULT: Int = Constant.PORT_SERVER_DEFAULT

        // 调试开关：开启后服务端信任所有连接，默认关闭。
        const val PROXY_TRUST_ALL_DEFAULT: Boolean = false
    }
}
