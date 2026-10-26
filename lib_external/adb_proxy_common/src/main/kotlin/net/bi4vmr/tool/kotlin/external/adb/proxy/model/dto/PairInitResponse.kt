package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 配对第一步响应。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
@Serializable
data class PairInitResponse(

    /**
     * 配对会话 ID 。
     */
    val pairId: String = "",

    /**
     * 服务端随机数（十六进制）。
     */
    val serverNonce: String = ""
)
