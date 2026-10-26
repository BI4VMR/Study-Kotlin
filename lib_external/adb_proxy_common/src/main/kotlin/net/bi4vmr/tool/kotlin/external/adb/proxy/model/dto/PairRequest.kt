package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 配对第一步请求参数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
@Serializable
data class PairRequest(

    /**
     * 客户端唯一标识。
     */
    val clientId: String = "",

    /**
     * 客户端名称，用于在服务端界面展示。
     */
    val clientName: String = "",

    /**
     * 客户端随机数的承诺值，参见 `PairingCode.commit()` 。
     */
    val commit: String = ""
)
