package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto

import kotlinx.serialization.Serializable

/**
 * 文件推送事件（服务端 -> 客户端）。
 *
 * 通过 [net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs.ADB_FILE_PUSH] 通道以 JSON 文本帧发送。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class FilePushEvent(

    /**
     * 事件类型。
     */
    val type: String,

    /**
     * 已推送至设备的数据量（字节）。
     */
    val size: Long = 0L,

    /**
     * 文件总大小（字节）。
     */
    val total: Long = 0L,

    /**
     * 传输是否被取消。仅 [TYPE_END] 事件有效。
     */
    val cancel: Boolean = false,

    /**
     * 错误信息。仅 [TYPE_ERROR] 事件有效。
     */
    val message: String = ""
) {

    companion object {

        /**
         * 服务端已接收完整文件，开始推送至设备。
         */
        const val TYPE_START: String = "start"

        /**
         * 设备端推送进度更新。
         */
        const val TYPE_PROGRESS: String = "progress"

        /**
         * 推送结束。
         */
        const val TYPE_END: String = "end"

        /**
         * 推送失败。
         */
        const val TYPE_ERROR: String = "error"

        /**
         * 客户端上行指令：文件数据发送完毕。
         */
        const val CMD_EOF: String = "eof"

        /**
         * 客户端上行指令：取消传输。
         */
        const val CMD_CANCEL: String = "cancel"
    }
}
