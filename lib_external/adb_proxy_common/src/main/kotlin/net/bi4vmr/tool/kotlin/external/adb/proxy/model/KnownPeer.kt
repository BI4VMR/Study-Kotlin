package net.bi4vmr.tool.kotlin.external.adb.proxy.model

/**
 * 已知对端。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
data class KnownPeer(

    val id: String,

    /**
     * 客户端证书的 SHA-256 指纹（十六进制小写），作为客户端的唯一身份。
     */
    val fingerprint: String,

    /**
     * 配对时间（毫秒时间戳）。
     */
    val pairedAt: Long
)
