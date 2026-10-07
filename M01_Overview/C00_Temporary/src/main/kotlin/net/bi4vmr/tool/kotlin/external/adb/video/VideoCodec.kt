package net.bi4vmr.tool.kotlin.external.adb.video

import org.bytedeco.ffmpeg.global.avcodec

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
     * FFmpeg 编码器 ID。
     */
    val ffID: Int
) {

    /**
     * H.264 编码。
     */
    H264("h264", avcodec.AV_CODEC_ID_H264),

    /**
     * H.265 编码。
     */
    H265("h265", avcodec.AV_CODEC_ID_HEVC),

    /**
     * AV1 编码。
     */
    AV1("av1", avcodec.AV_CODEC_ID_AV1),

    /**
     * VP8 编码。
     *
     * Google 推出的 H.264 开源替代方案，从 Scrcpy v4.1 版本开始支持。
     */
    VP8("vp8", avcodec.AV_CODEC_ID_VP8),

    /**
     * VP9 编码。
     *
     * Google 推出的 H.265 开源替代方案，从 Scrcpy v4.1 版本开始支持。
     */
    VP9("vp9", avcodec.AV_CODEC_ID_VP9);
}
