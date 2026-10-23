package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

import kotlinx.serialization.Serializable
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec

/**
 * 请求参数：开启远程投屏。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBCastStartRequest(

    /**
     * 设备序列号。
     */
    val sn: String = "",

    val displayID: String = "0",
    val videoCodec: VideoCodec = VideoCodec.H264,
    val bitRate: Int = 8_000_000,
    val maxSize: Int = -1,
    val maxFPS: Int = 360,
    val enableAudio: Boolean = false,
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val audioBitRate: Int = 128_000
)