package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto

import kotlinx.serialization.Serializable

/**
 * 响应消息：服务端信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ServerInfoResponse(

    /**
     * 服务端 ID 。
     */
    val id: String = "",

    /**
     * 服务端名称。
     */
    val name: String = "",

    /**
     * 协议版本。
     */
    val version: String = ""
)
