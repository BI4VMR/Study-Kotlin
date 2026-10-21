package net.bi4vmr.tool.kotlin.external.adb.cast.video

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
     * FFmpeg 解码器 ID 。
     *
     * 本类作为基础类不应当传递 FFmpeg 依赖给调用者，因此该值直接使用数值表示。
     */
    val ffmpegID: Int
) {

    /**
     * H.264 编码。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_H264` 。
     */
    H264("h264", 27),

    /**
     * H.265 编码。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_HEVC` 。
     */
    H265("h265", 173),

    /**
     * AV1 编码。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_AV1` 。
     */
    AV1("av1", 226),

    /**
     * VP8 编码。
     *
     * Google 推出的 H.264 开源替代方案，从 Scrcpy v4.1 版本开始支持。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_VP8` 。
     */
    VP8("vp8", 139),

    /**
     * VP9 编码。
     *
     * Google 推出的 H.265 开源替代方案，从 Scrcpy v4.1 版本开始支持。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_VP9` 。
     */
    VP9("vp9", 167);
}
