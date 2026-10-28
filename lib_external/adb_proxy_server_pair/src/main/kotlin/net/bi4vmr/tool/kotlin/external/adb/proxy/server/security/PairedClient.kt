package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

/**
 * 已配对的客户端。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
data class PairedClient(

    /**
     * 客户端证书的 SHA-256 指纹（十六进制小写），作为客户端的唯一身份。
     */
    val fingerprint: String,

    /**
     * 客户端名称，仅用于界面展示，不参与认证。
     */
    val name: String,

    /**
     * 配对时间（毫秒时间戳）。
     */
    val pairedAt: Long
)
