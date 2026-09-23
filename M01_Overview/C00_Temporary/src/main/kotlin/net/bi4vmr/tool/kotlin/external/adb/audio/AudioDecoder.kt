package net.bi4vmr.tool.kotlin.external.adb.audio

import net.bi4vmr.tool.kotlin.external.adb.ADBCastContext
import net.bi4vmr.tool.kotlin.external.adb.ADBCastEventListener
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.global.swresample.*
import org.bytedeco.ffmpeg.swresample.SwrContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer

/**
 * 音频解码器。
 *
 * <p>解码目标统一为 48kHz 双声道 S16 交错格式的 PCM 数据，便于上层直接播放。</p>
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal class AudioDecoder {

    private var codecCtx: AVCodecContext? = null
    private var swrCtx: SwrContext? = null
    private var avPacket: AVPacket? = null
    private var avFrame: AVFrame? = null

    private var outChannels: Int = 2
    private var outSampleRate: Int = 48000

    private var listener: ADBCastEventListener? = null

    fun init(context: ADBCastContext) {
        listener = context.listener

        val codec = avcodec_find_decoder(context.audioCodec.ffID)
        val avCtx = avcodec_alloc_context3(codec) ?: throw IllegalStateException("Load codec context failed!")

        // 设置音频参数，对应 C 客户端 demuxer.c 中硬编码的音频属性。
        avCtx.apply {
            // 立体声
            av_channel_layout_default(ch_layout(), 2)
            sample_rate(48000)
            if (context.audioCodec.name == "FLAC") {
                // FLAC 解码器不会自动设置采样格式，需要手动指定。
                sample_fmt(AV_SAMPLE_FMT_S16)
            }
        }

        val result = avcodec_open2(avCtx, codec, null as AVDictionary?)
        if (result < 0) {
            throw IllegalStateException("Load codec failed!")
        }

        codecCtx = avCtx
        avPacket = av_packet_alloc()
        avFrame = av_frame_alloc()

        // 初始化重采样器，将解码后的音频统一转为 S16 交错格式。
        val swr = swr_alloc()
        val outLayout = AVChannelLayout()
        av_channel_layout_default(outLayout, outChannels)
        val inLayout = AVChannelLayout()
        av_channel_layout_default(inLayout, avCtx.ch_layout().nb_channels())

        val configResult = swr_alloc_set_opts2(
            swr,
            outLayout,
            AV_SAMPLE_FMT_S16,
            outSampleRate,
            inLayout,
            avCtx.sample_fmt(),
            avCtx.sample_rate(),
            0,
            null
        )
        if (configResult < 0) {
            swr_free(swr)
            throw IllegalStateException("Configure resampler failed!")
        }

        if (swr_init(swr) < 0) {
            swr_free(swr)
            throw IllegalStateException("Init resampler failed!")
        }

        swrCtx = swr
    }

    fun decode(data: ByteArray, pts: Long) {
        val mediaCtx = codecCtx
        val packet = avPacket
        val frame = avFrame
        val swr = swrCtx
        if (mediaCtx == null || packet == null || frame == null || swr == null) {
            throw IllegalStateException("Had you call #init at first?")
        }

        // 配置包（如 OPUS 的 OpusHead）无需送入解码器，解码参数已在初始化时硬编码。
        if (pts == AV_NOPTS_VALUE) {
            return
        }

        packet.apply {
            av_new_packet(this, data.size)
            data().put(data, 0, data.size)
            size(data.size)
            pts(pts)
            dts(pts)
        }

        val sendResult = avcodec_send_packet(mediaCtx, packet)
        av_packet_unref(packet)
        if (sendResult != 0) {
            println("Send packet to codec failed! Code:[$sendResult]")
            return
        }

        // 循环接收解码后的帧
        while (true) {
            val receiveResult = avcodec_receive_frame(mediaCtx, frame)
            // 非 `0` 表示出错或没有新的数据，停止本数据包的解码工作。
            if (receiveResult != 0) break

            val nbSamples = frame.nb_samples()
            if (nbSamples <= 0) break

            // 重采样为 S16 交错格式
            val outSamples = swr_get_out_samples(swr, nbSamples)
            val outBytesCount = outSamples * outChannels * 2
            val outBytes = BytePointer(outBytesCount.toLong())
            // `out` 参数类型为 `uint8_t **`，需要构造一个指针数组，并将输出缓冲区指针写入第一个元素。
            val outPlanes = PointerPointer<BytePointer>(1L)
            outPlanes.put(0, outBytes)
            val samplesConverted = swr_convert(swr, outPlanes, outSamples, frame.data(), nbSamples)
            if (samplesConverted <= 0) {
                outPlanes.deallocate()
                outBytes.deallocate()
                break
            }

            val bytesCount = samplesConverted * outChannels * 2
            val pcm = ByteArray(bytesCount)
            outBytes.get(pcm)
            outPlanes.deallocate()
            outBytes.deallocate()

            listener?.onAudioData(pcm, outChannels, outSampleRate)
        }
    }

    fun release() {
        avPacket?.let { av_packet_free(it) }
        avFrame?.let { av_frame_free(it) }
        codecCtx?.let { avcodec_free_context(it) }
        swrCtx?.let { swr_free(it) }

        avPacket = null
        avFrame = null
        codecCtx = null
        swrCtx = null
    }
}
