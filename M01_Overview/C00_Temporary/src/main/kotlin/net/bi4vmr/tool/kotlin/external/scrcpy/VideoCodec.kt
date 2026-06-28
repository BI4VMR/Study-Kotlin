package net.bi4vmr.tool.kotlin.external.scrcpy

import org.bytedeco.ffmpeg.global.avcodec.*

/**
 * 视频编码类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class VideoCodec(

    /**
     * 命令行参数。
     */
    val cli: String,

    /**
     * FFmpeg编码器ID。
     */
    val ffID: Int
) {
    H264("h264", AV_CODEC_ID_H264),

    H265("h265", AV_CODEC_ID_HEVC),

    AV1("av1", AV_CODEC_ID_AV1);
}
