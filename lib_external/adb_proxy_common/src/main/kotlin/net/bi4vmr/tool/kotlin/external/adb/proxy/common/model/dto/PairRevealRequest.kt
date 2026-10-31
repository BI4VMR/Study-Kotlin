package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto

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
     * 客户端随机数。
     */
    val clientNonce: String = ""
)
