package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig

/**
 * 运行时配置。
 *
 * 客户端启动时复制部分配置信息，防止运行时配置被修改导致非预期的错误。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class RunningConfig(

    /**
     * 客户端名称。
     */
    val name: String,

    /**
     * 目标地址。
     */
    val host: String,

    /**
     * 目标端口。
     */
    val port: Int,

    /**
     * 是否启用加密连接。
     */
    val enableTLS: Boolean,

    /**
     * SSL 证书配置。
     */
    val sslConfig: SSLConfig?,
)
