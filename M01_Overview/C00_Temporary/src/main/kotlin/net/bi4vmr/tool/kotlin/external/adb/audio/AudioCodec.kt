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
     * FFmpeg解码器ID。
     */
    val ffID: Int
) {
    OPUS("opus", avcodec.AV_CODEC_ID_OPUS),

    AAC("aac", avcodec.AV_CODEC_ID_AAC),

    FLAC("flac", avcodec.AV_CODEC_ID_FLAC);
}