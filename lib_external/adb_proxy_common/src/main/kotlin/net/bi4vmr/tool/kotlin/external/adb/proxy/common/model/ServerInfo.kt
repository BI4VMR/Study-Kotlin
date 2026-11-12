package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model

import kotlinx.serialization.Serializable

/**
 * 响应消息：服务端信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ServerInfo(

    /**
     * 节点名称。
     */
    val name: String,

    /**
     * 协议版本。
     */
    val version: String
)
