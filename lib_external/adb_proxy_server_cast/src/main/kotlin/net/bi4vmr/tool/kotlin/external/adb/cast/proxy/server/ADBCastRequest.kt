package net.bi4vmr.tool.kotlin.external.adb.cast.proxy.server

import kotlinx.serialization.Serializable

/**
 * 请求参数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBCastRequest(

    /**
     * 设备序列号。
     */
    val sn: String = ""
)
