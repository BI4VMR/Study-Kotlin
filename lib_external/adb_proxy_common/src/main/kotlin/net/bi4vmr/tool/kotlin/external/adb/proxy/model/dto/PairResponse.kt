package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 配对结果。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
@Serializable
data class PairResponse(

    /**
     * 访问令牌，后续请求需通过 `Authorization: Bearer <token>` 携带。
     */
    val token: String = ""
)
