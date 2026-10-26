package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 配对第二步请求参数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
@Serializable
data class PairRevealRequest(

    /**
     * 配对会话 ID 。
     */
    val pairId: String = "",

    /**
     * 客户端随机数（十六进制），其摘要必须与第一步提交的承诺一致。
     */
    val clientNonce: String = ""
)
