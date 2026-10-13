package net.bi4vmr.tool.kotlin.external.adb.cast.audio

/**
 * 音频编码类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class AudioCodec(

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
     * AAC 编码。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_AAC` 。
     */
    AAC("aac", 0x15000 + 2),

    /**
     * OPUS 编码。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_OPUS` 。
     */
    OPUS("opus", 0x15000 + 60),

    /**
     * FLAC 编码。
     *
     * FFmpeg 中的定义为 `avcodec.AV_CODEC_ID_FLAC` 。
     */
    FLAC("flac", 0x15000 + 12);
}
