package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

/**
 * 已配对的客户端。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
data class PairedClient(

    /**
     * 客户端唯一标识。
     */
    val id: String,

    /**
     * 客户端名称。
     */
    val name: String,

    /**
     * 访问令牌的 SHA-256 摘要（十六进制），服务端不保存令牌明文。
     */
    val tokenHash: String,

    /**
     * 配对时间（毫秒时间戳）。
     */
    val pairedAt: Long
)
