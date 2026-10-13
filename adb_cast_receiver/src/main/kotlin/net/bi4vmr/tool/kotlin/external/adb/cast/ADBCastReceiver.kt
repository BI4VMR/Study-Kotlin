package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.java.common.base.net.NetUtil
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoProtocolParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
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
     * 接收端监听任务列表。
     */
    private val receivers: MutableList<ADBCastMediaListener, Receiver> = mutableMapOf()


    /**
     * 启动屏幕投射监听任务（接收端）。
     *
     * 监听成功后，发送端连接并开始转发数据，音视频帧与控制器通过 [listener] 回调。
     *
     * @param[listeningHost] 监听地址。
     * @param[listeningPort] 监听端口，传入 `0` 表示由系统分配。
     * @param[listener] 事件监听器。
     * @return 实际监听的端口；监听失败时回调 [net.bi4vmr.tool.kotlin.external.adb.cast.common.ADBCastEventListener.onVideoError] 并返回 `-1` 。
     */
    @JvmStatic
    fun start(
        host: String,
        port: Int,
        listener: ADBCastMediaListener
    ) {
        thread {
            try {
                val socket = Socket(host, port)
            } catch (e: Exception) {
                listener.onVideoError(e)
            }
        }


        val receiver = Receiver(serverSocket, listener)
        synchronized(receivers) {
            if (receivers.containsKey(listener)) {
                runCatching { serverSocket.close() }
                listener.onVideoError(IllegalStateException("Listening task already running!"))
                return -1
            }

            receivers[listener] = receiver
        }

        logger.info("Listening at [$listeningHost:${serverSocket.localPort}].")
        thread {
            try {
                while (!serverSocket.isClosed) {
                    val socket = serverSocket.accept()
                    thread { handleConnection(receiver, socket) }
                }
            } catch (e: Exception) {
                if (!receiver.stopped) {
                    listener.onVideoError(e)
                }
            } finally {
                stopReceiver(listener)
            }
        }

        return serverSocket.localPort
    }

    private fun stopReceiver(listener: ADBCastMediaListener) {
        val receiver = synchronized(receivers) { receivers.remove(listener) } ?: return
        receiver.stopped = true
        runCatching { receiver.serverSocket.close() }

        val sessions = synchronized(receiver.sessions) { receiver.sessions.values.toList() }
        sessions.forEach { clearSession(receiver, it) }
    }

    private fun handleConnection(receiver: Receiver, socket: Socket) {
        val listener = receiver.listener
        var context: ADBRemoteReceiveContext? = null
        var channel = -1
        try {
            socket.tcpNoDelay = true
            val input = DataInputStream(socket.getInputStream())
            val sessionID = input.readLong()
            channel = input.readUnsignedByte()
            val videoCodec = VideoCodec.valueOf(input.readUTF())
            val audioName = input.readUTF()
            val audioCodec = if (audioName.isEmpty()) null else AudioCodec.valueOf(audioName)

            val ctx = synchronized(receiver.sessions) {
                receiver.sessions.getOrPut(sessionID) {
                    ADBRemoteReceiveContext(sessionID, listener, videoCodec, audioCodec)
                }
            }
            context = ctx
            if (ctx.stopped || receiver.stopped) {
                socket.close()
                return
            }

            when (channel) {
                CHANNEL_VIDEO -> {
                    ctx.videoSocket = socket
                    ctx.videoThread = Thread.currentThread()
                    val decoder = VideoDecoder()
                    decoder.init(ctx)
                    ctx.videoDecoder = decoder
                    VideoProtocolParser.parse(ctx)
                }

                CHANNEL_AUDIO -> {
                    if (audioCodec == null) {
                        throw IllegalStateException("Audio channel is disabled!")
                    }

                    ctx.audioSocket = socket
                    ctx.audioThread = Thread.currentThread()
                    val decoder = AudioDecoder()
                    decoder.init(ctx)
                    ctx.audioDecoder = decoder
                    AudioProtocolParser.parse(ctx)
                }

                CHANNEL_CONTROL -> {
                    ctx.controlSocket = socket
                    val controller = InputController(socket.getOutputStream())
                    ctx.inputController = controller
                    listener.onControlReady(controller)

                    // 设备可能回传控制消息，此处仅丢弃，防止发送缓冲区被占满。
                    val buffer = ByteArray(1024)
                    while (input.read(buffer) >= 0) {
                        // 丢弃
                    }
                }

                else -> throw IllegalStateException("Unknown channel type [$channel]!")
            }
        } catch (e: Exception) {
            if (context?.stopped != true && !receiver.stopped) {
                if (channel == CHANNEL_AUDIO) {
                    listener.onAudioError(e)
                } else {
                    listener.onVideoError(e)
                }
            }
        } finally {
            runCatching { socket.close() }
            // 任一通道结束则整个会话结束。
            context?.let { clearSession(receiver, it) }
        }
    }

    private fun clearSession(receiver: Receiver, context: ADBRemoteReceiveContext) {
        context.apply {
            if (stopped) {
                return
            }

            logger.debug("ClearContext(Session).")
            stopped = true
            synchronized(receiver.sessions) { receiver.sessions.remove(sessionID) }

            runCatching { videoSocket?.close() }
            runCatching { audioSocket?.close() }
            runCatching { controlSocket?.close() }

            // 等待解析线程退出后再释放解码器，防止 Native 层访问已释放的资源。
            videoThread?.let { joinThread(it) }
            audioThread?.let { joinThread(it) }

            videoDecoder?.release()
            audioDecoder?.release()
        }
    }

    private fun joinThread(target: Thread, timeout: Long = 2000L) {
        if (target === Thread.currentThread()) {
            return
        }

        target.interrupt()
        try {
            target.join(timeout)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    /* 工具方法 */

    private fun findAvailablePort(): Int {
        val attempts = FORWARD_PORT_END - FORWARD_PORT_START
        repeat(attempts) {
            val port = forwardPort.getAndUpdate { value ->
                if (value + 1 >= FORWARD_PORT_END) FORWARD_PORT_START else value + 1
            }

            logger.debug("Attempt to find available port [$port].")
            if (NetUtil.isPortAvailable(NetUtil.IP_LOOPBACK, port)) {
                logger.info("Port [$port] is available.")
                return port
            }
        }

        throw IOException("No available port in [$FORWARD_PORT_START, $FORWARD_PORT_END)!")
    }

    private fun connectForHandshake(
        host: String,
        port: Int,
        retryCount: Int = 50,
        delay: Long = 200L,
        logTag: String? = null
    ): Socket {
        repeat(retryCount) { time ->
            logger.debug("Attempt to connect${if (logTag != null) " [$logTag]" else ""} socket for [${time + 1}] time.")
            var socket: Socket? = null
            try {
                socket = Socket(host, port)
                // ADB 转发通道开启后连接不会失败，但服务端未就绪读取会出现异常，若能读出握手字节说明服务端就绪。
                val first = DataInputStream(socket.getInputStream()).readByte()
                if (first == 0x00.toByte()) {
                    return socket
                } else {
                    throw IOException("Unexpected handshake byte! We expect 0x00, but got [0x${first.toString(16)}].")
                }
            } catch (e: Exception) {
                runCatching { socket?.close() }
                if (time == retryCount - 1) {
                    throw e
                } else {
                    Thread.sleep(delay)
                }
            }
        }

        throw IllegalStateException("Unreachable code!")
    }

    private fun connect(
        host: String,
        port: Int,
        retryCount: Int = 50,
        delay: Long = 200L,
        logTag: String? = null
    ): Socket {
        repeat(retryCount) { time ->
            logger.debug("Attempt to connect${if (logTag != null) " [$logTag]" else ""} socket for [${time + 1}] time.")
            try {
                return Socket(host, port)
            } catch (e: Exception) {
                if (time == retryCount - 1) {
                    throw e
                } else {
                    Thread.sleep(delay)
                }
            }
        }

        throw IllegalStateException("Unreachable code!")
    }
}
