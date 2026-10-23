package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoProtocolParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.Socket
import kotlin.concurrent.thread

/**
 * ADB 屏幕投射管理（接收端）。
 *
 * 从 `ADBCastTransfer` 接收 Scrcpy 数据并进行解码，可显示投屏画面或播放音频。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBCastReceiver {

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBCastReceiver::class.java)


    /**
     * 启动屏幕投射接收任务。
     *
     * @param[host] 发送端地址。
     * @param[port] 发送端端口。
     * @param[listener] 事件监听器。
     */
    @JvmStatic
    fun start(
        host: String,
        port: Int,
        listener: ADBCastMediaListener,
        videoCodec: VideoCodec = VideoCodec.H264,
        enableAudio: Boolean = false,
        audioCodec: AudioCodec = AudioCodec.AAC
    ) {
        /* 创建投屏任务 */
        val context = ADBCastReceiverContext(
            listener = listener,
            deviceSN = "1",
            videoCodec = videoCodec,
            audioCodec = audioCodec
        )

        thread {
            try {
                /* 建立视频通道（第一个 TCP 连接） */
                // 发送端与 Android 设备连接时已消耗握手字节，因此接收端无需处理该字节，直接解析后续数据包即可。
                val videoSocket = Socket(host, port)
                videoSocket.tcpNoDelay = true
                context.videoSocket = videoSocket

                // 循环读取视频通道数据并进行解码
                thread {
                    try {
                        val videoDecoder = VideoDecoder()
                        videoDecoder.init(context)
                        context.videoDecoder = videoDecoder
                        VideoProtocolParser.parse(context)
                    } catch (e: Exception) {
                        listener.onVideoError(e)
                    }
                }

                /*
                 * 建立音频通道（第二个 TCP 连接）
                 *
                 * 服务端按照客户端连接顺序分配数据通道，分别为：视频 -> 音频 -> 控制，未启用音频时，第二通道为控制通道。
                 */
                if (enableAudio) {
                    val audioSocket = Socket(host, port)
                    audioSocket.tcpNoDelay = true
                    context.audioSocket = audioSocket

                    thread {
                        try {
                            val audioDecoder = AudioDecoder()
                            audioDecoder.init(context)
                            context.audioDecoder = audioDecoder
                            AudioProtocolParser.parse(context)
                        } catch (e: Exception) {
                            listener.onAudioError(e)
                        }
                    }
                }

                /* 建立远程控制通道（第三个 TCP 连接） */
                val controlSocket = Socket(host, port)
                controlSocket.tcpNoDelay = true
                context.controlSocket = controlSocket

                val inputController = InputController(controlSocket.getOutputStream())
                context.inputController = inputController
                listener.onControlReady(inputController)
            } catch (e: Exception) {
                listener.onConnectError(e)
            }
        }
    }

    /**
     * 停止屏幕投射接收任务。
     *
     * @param[listener] 事件监听器。
     */
    @JvmStatic
    fun stop(listener: ADBCastMediaListener) {
        // TODO
    }
}
