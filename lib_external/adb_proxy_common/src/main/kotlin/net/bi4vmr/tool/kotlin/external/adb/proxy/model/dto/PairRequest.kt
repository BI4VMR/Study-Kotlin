package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable

/**
 * 配对第一步请求参数。
 *
 * 客户端身份由 TLS 握手中出示的客户端证书确定，不在请求体中声明。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
@Serializable
data class PairRequest(

    /**
     * 客户端名称，仅用于在服务端界面展示，不参与认证。
     */
    val clientName: String = "",

    /**
     * 客户端随机数的承诺值，参见 `PairUtil.getCommit()` 。
     */
    val commit: String = ""
)
