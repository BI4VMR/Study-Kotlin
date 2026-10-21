package net.bi4vmr.tool.kotlin.external.adb.cast.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 请求参数：开启远程投屏。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBCastStartRequest(

    /**
     * 设备序列号。
     */
    val sn: String = ""
)
