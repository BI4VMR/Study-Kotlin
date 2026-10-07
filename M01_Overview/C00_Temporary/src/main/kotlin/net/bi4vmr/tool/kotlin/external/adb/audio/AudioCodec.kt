package net.bi4vmr.tool.kotlin.external.adb.audio

import org.bytedeco.ffmpeg.global.avcodec

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
     */
    val ffID: Int
) {
    /**
     * AAC 编码。
     */
    AAC("aac", avcodec.AV_CODEC_ID_AAC),

    /**
     * OPUS 编码。
     */
    OPUS("opus", avcodec.AV_CODEC_ID_OPUS),

    /**
     * FLAC 编码。
     */
    FLAC("flac", avcodec.AV_CODEC_ID_FLAC);
}
