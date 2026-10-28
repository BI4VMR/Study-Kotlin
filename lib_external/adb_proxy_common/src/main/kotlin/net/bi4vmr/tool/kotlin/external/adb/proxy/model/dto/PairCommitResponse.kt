package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

/**
 * 响应：配对第一阶段。
 *
 * 服务端回复本端身份信息的承诺值。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class PairCommitResponse(

    /**
     * 会话 ID 。
     *
     * 客户端发送二阶段请求时需要携带该信息。
     */
    val sessionID: String = "",

    /**
     * 服务端证书与随机数的消息摘要。
     */
    val serverHash: String = ""
)
