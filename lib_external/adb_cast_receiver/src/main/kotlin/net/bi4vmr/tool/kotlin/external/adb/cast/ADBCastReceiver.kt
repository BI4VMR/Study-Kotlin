package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
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
 * 从 [ADBCastTransfer] 接收 Scrcpy 数据并进行解码，可显示投屏画面或播放音频。
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
     * 启动屏幕投射接收任务（接收端）。
     *
     * 连接发送端的监听端口，并依次建立视频、音频（若启用）与控制三个连接，音视频帧与控制器通过 [listener] 回调。
     *
     * @param[host] 发送端地址。
     * @param[port] 发送端监听的端口，由 [ADBCastTransfer.start] 返回。
     * @param[enableAudio] 发送端是否启用音频，需与发送端保持一致。
     * @param[listener] 事件监听器。
     */
    @JvmStatic
    fun start(
        host: String,
        port: Int,
        listener: ADBCastMediaListener,
        enableAudio: Boolean = false
    ) {
        val context = ADBCastReceiverContext(
            listener = listener,
            deviceID = "1",
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC
        )

        thread {
            try {
                // 连接顺序固定：视频 -> 音频（若启用） -> 控制，发送端按此顺序区分通道。
                val videoSocket = Socket(host, port).apply { tcpNoDelay = true }
                context.videoSocket = videoSocket

                val audioSocket = if (enableAudio) Socket(host, port).apply { tcpNoDelay = true } else null
                context.audioSocket = audioSocket

                val controlSocket = Socket(host, port).apply { tcpNoDelay = true }
                context.controlSocket = controlSocket

                // 控制器需先于视频解析器创建，视频尺寸变化时会更新控制器的坐标基准。
                val inputController = InputController(controlSocket.getOutputStream())
                context.inputController = inputController
                listener.onControlReady(inputController)

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

                if (audioSocket != null) {
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
            } catch (e: Exception) {
                listener.onConnectError(e)
            }
        }
    }

    /**
     * 停止屏幕投射接收任务（接收端）。
     *
     * 关闭视频、音频与控制三个连接，并释放资源。
     *
     * @param[listener] 事件监听器。
     */
    @JvmStatic
    fun stop(listener: ADBCastMediaListener) {
        // listener.onStop()
    }
}
