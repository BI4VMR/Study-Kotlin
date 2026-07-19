package net.bi4vmr.tool.kotlin.external.cast.video

import net.bi4vmr.tool.kotlin.external.cast.ScreenCastContext
import net.bi4vmr.tool.kotlin.external.cast.ScreenCastEventListener
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecContext.FF_THREAD_FRAME
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec.*
import org.bytedeco.ffmpeg.global.avutil.*

/**
 * 视频解码器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal class VideoDecoder {

    private var codecCtx: AVCodecContext? = null
    private var avPacket: AVPacket? = null
    private var avFrame: AVFrame? = null

    private var listener: ScreenCastEventListener? = null

    fun init(context: ScreenCastContext) {
        listener = context.listener

        val codec = avcodec_find_decoder(context.videoCodec.ffID)
        val avCtx = avcodec_alloc_context3(codec) ?: throw IllegalStateException("Load codec context failed!")

        avCtx.apply {
            // 启用低时延
            flags(flags() or AV_CODEC_FLAG_LOW_DELAY)
            // 启用帧级多线程解码
            thread_type(FF_THREAD_FRAME)
            // 自动选择解码线程数量
            thread_count(0)
        }

        val result = avcodec_open2(avCtx, codec, null as AVDictionary?)
        if (result < 0) {
            throw IllegalStateException("Load codec failed!")
        }

        codecCtx = avCtx
        avPacket = av_packet_alloc()
        avFrame = av_frame_alloc()
    }

    fun decode(data: ByteArray, pts: Long, keyFrame: Boolean) {
        val mediaCtx = codecCtx
        val packet = avPacket
        val frame = avFrame
        if (mediaCtx == null || packet == null || frame == null) {
            throw IllegalStateException("Had you call #init at first?")
        }

        packet.apply {
            av_new_packet(this, data.size)
            data().put(data, 0, data.size)
            size(data.size)
            pts(pts)
            dts(pts)
            if (keyFrame) {
                flags(flags() or AV_PKT_FLAG_KEY)
            }
        }

        val sendResult = avcodec_send_packet(mediaCtx, packet)
        av_packet_unref(packet)
        if (sendResult != 0) {
            // 发送SPS/PPS配置包会返回非 `0` 的值，但不影响后续使用，因此这种情况不输出日志。
            if (pts != AV_NOPTS_VALUE) {
                println("Send packet to codec failed! Code:[$sendResult]")
            }
            return
        }

        // 循环接收解码后的帧
        while (true) {
            val receiveResult = avcodec_receive_frame(mediaCtx, frame)
            // 非 `0` 表示出错或没有新的数据，停止本数据包的解码工作。
            if (receiveResult != 0) break

            val width = frame.width()
            val height = frame.height()
            if (width <= 0 || height <= 0) break

            // 解析YUV数据
            val yStride: Int = frame.linesize(0)
            val uStride: Int = frame.linesize(1)
            val vStride: Int = frame.linesize(2)

            val yData = ByteArray(yStride * height)
            val uData = ByteArray(uStride * (height / 2))
            val vData = ByteArray(vStride * (height / 2))

            frame.data(0).get(yData)
            frame.data(1).get(uData)
            frame.data(2).get(vData)

            listener?.onNewFrame(yData, yStride, uData, uStride, vData, vStride)
        }
    }

    fun release() {
        avPacket?.let { av_packet_free(it) }
        avFrame?.let { av_frame_free(it) }
        codecCtx?.let { avcodec_free_context(it) }

        avPacket = null
        avFrame = null
        codecCtx = null
    }
}
