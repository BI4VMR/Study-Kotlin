package net.bi4vmr.tool.kotlin.external.adb.proxy.http

import kotlinx.serialization.Serializable

/**
 * 远程 ADB 命令请求参数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBExecRequest(
    val sn: String = "",
    val command: String = ":",
    val redirectError: Boolean = true,
    val charset: String? = null
)
