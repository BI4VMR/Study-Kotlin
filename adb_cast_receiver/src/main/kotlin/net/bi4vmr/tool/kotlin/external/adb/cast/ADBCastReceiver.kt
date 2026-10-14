package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoProtocolParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.net.Socket
import kotlin.concurrent.thread

/**
 * ADB 屏幕投射管理（接收端）。
 *
 * 从 [ADBCastTransfer] 接收 Scrcpy 数据并进行解码，可显示投屏画面或播放音频。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBCastReceiver {

    /**
     * 数据类型：视频通道。
     */
    private const val TYPE_VIDEO_DATA: Int = 101

    /**
     * 数据类型：视频通道错误信息。
     */
    private const val TYPE_VIDEO_ERROR: Int = 102

    /**
     * 数据类型：音频通道。
     */
    private const val TYPE_AUDIO_DATA: Int = 201

    /**
     * 数据类型：音频通道错误信息。
     */
    private const val TYPE_AUDIO_ERROR: Int = 202

    /**
     * 数据类型：控制通道。
     */
    private const val TYPE_CONTROL_DATA: Int = 301

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBCastReceiver::class.java)


    /**
     * 启动屏幕投射监听任务（接收端）。
     *
     * 监听成功后，发送端连接并开始转发数据，音视频帧与控制器通过 [listener] 回调。
     *
     * @param[host] 监听地址。
     * @param[port] 监听端口，传入 `0` 表示由系统分配。
     * @param[listener] 事件监听器。
     */
    @JvmStatic
    fun start(
        host: String,
        port: Int,
        listener: ADBCastMediaListener
    ) {
        val context = ADBCastReceiverContext(
            listener = listener,
            deviceID = "1",
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC
        )
        val videoDecoder = VideoDecoder()
        videoDecoder.init(context)
        context.videoDecoder = videoDecoder
        VideoProtocolParser.parse(context)

        thread {
            try {
                val socket = Socket(host, port)
                DataInputStream(socket.getInputStream()).use {
                    while (true) {
                        val type = it.readInt()
                        println(type)
                        if (type == TYPE_VIDEO_DATA) {

                        }
                    }
                }
            } catch (e: Exception) {
                listener.onConnectError(e)
            }
        }
    }
}
