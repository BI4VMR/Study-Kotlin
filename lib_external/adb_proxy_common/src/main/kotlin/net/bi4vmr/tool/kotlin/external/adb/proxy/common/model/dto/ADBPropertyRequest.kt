package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto

import kotlinx.serialization.Serializable

/**
 * 请求：查询设备属性。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBPropertyRequest(

    /**
     * 设备序列号。
     */
    val sn: String,

    /**
     * 属性名称。
     */
    val key: String,

    /**
     * 字符集。
     *
     * 空值表示由服务端自动侦测。
     */
    val charset: String? = null
)
