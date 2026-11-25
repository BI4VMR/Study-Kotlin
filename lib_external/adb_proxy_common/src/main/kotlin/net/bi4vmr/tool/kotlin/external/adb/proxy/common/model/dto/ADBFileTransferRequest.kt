package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto

import kotlinx.serialization.Serializable

/**
 * 请求：文件传输。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBFileTransferRequest(

    /**
     * 设备序列号。
     */
    val sn: String,

    /**
     * 本地路径。
     */
    val local: String,

    /**
     * 远程路径。
     */
    val remote: String
)
