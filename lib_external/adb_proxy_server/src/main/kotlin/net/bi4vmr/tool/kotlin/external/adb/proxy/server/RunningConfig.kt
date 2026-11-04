package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import kotlinx.serialization.Serializable
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairRequestListener
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairedPeerStore
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig

/**
 * 运行时配置。
 *
 * 服务器启动时复制部分配置信息，防止运行时配置被修改导致非预期的错误。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class RunningConfig(

    /**
     * 服务端 ID 。
     */
    val id: String,

    /**
     * 服务端名称。
     */
    val name: String,

    /**
     * 监听地址。
     */
    val address: String,

    /**
     * 监听端口。
     */
    val port: Int,

    /**
     * SSL 证书配置。
     */
    val sslConfig: SSLConfig?,

    /**
     * 配对模块：已知对等体信息存储。
     */
    val pairedPeerStore: PairedPeerStore,

    /**
     * 配对模块：配对请求监听器。
     */
    val pairRequestListener: PairRequestListener?
)
