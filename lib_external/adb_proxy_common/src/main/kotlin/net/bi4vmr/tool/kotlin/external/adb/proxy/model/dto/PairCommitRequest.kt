package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

/**
 * 请求：配对第一阶段。
 *
 * 客户端发送本端身份信息的承诺值。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class PairCommitRequest(

    /**
     * 客户端证书与随机数的消息摘要。
     */
    val clientHash: String = ""
)
