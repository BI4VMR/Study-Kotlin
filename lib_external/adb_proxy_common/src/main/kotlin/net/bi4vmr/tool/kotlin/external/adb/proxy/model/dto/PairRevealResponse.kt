package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 响应：配对第二阶段。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class PairRevealResponse(

    /**
     * 服务端随机数。
     */
    val serverNonce: String = ""
)
