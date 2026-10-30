package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 请求：配对第二阶段。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class PairRevealRequest(

    /**
     * 会话 ID 。
     *
     * 第一阶段服务器返回的 ID 。
     */
    val sessionID: String = "",

    /**
     * 客户端随机数。
     */
    val clientNonce: String = ""
)
