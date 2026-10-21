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

    /**
     * 设备序列号。
     */
    val sn: String = "",

    /**
     * 命令语句。
     */
    val command: String = ":",

    /**
     * 是否重定向错误输出。
     */
    val redirectError: Boolean = true,

    /**
     * 字符集。
     *
     * 空值表示由服务端自动侦测。
     */
    val charset: String? = null
)
