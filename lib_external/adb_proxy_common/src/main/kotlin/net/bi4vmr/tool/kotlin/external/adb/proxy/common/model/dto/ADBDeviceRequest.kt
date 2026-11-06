package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto

import kotlinx.serialization.Serializable

/**
 * 请求：通用参数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBDeviceRequest(

    /**
     * 设备序列号。
     */
    val sn: String = ""
)
