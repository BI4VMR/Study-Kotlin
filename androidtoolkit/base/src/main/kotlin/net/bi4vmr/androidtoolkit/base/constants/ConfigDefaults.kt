package net.bi4vmr.androidtoolkit.base.constants

import net.bi4vmr.androidtoolkit.base.enums.ADBExecFileMode
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants

/**
 * 配置项默认值。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ConfigDefaults {

    const val COMMON_NODE_NAME: String = "AndroidToolkit"

    val ADB_EXEC_MODE: Int = ADBExecFileMode.INTERNAL.code

    const val PROXY_START_ON_INIT: Boolean = true

    const val PROXY_INTERFACE: String = "0.0.0.0"

    const val PROXY_PORT: Int = Constants.PORT_SERVER_DEFAULT

    const val PROXY_TLS: Boolean = true
}
