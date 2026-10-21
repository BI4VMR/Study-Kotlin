package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.net.NetUtil
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastManager.FORWARD_PORT_END
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastManager.FORWARD_PORT_START
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastManager.forwardPort
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.util.ScrcpyServerHelper
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * ADB 屏幕投射管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBCastManager {

    /**
     * ADB 端口转发的起始端口号。
     */
    private const val FORWARD_PORT_START: Int = 32768 + 1

    /**
     * ADB 端口转发的结束端口号。
     *
     * 端口号达到该值时，回到 [FORWARD_PORT_START] 重新递增尝试。
     */
    private const val FORWARD_PORT_END: Int = 33024


    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBCastManager::class.java)

    /**
     * 投屏任务列表。
     */
    private val tasks: MutableMap<ADBCastMediaListener, ADBCastContext> = mutableMapOf()

    /**
     * 上次使用的 ADB 转发端口。
     *
     * 每次建立新连接时，从当前端口之后一位尝试查找可用端口。
     */
    private val forwardPort: AtomicInteger = AtomicInteger(FORWARD_PORT_START)


    /**
     * 启动屏幕投射任务。
     *
     * @param[device] ADB 设备。
     * @param[listener] 事件监听器。用于接收媒体数据和错误信息等。
     * @param[displayID] 屏幕 ID 。默认为主屏。
     * @param[videoCodec] 视频编码。默认值为 H.264 。
     * @param[bitRate] 视频码率，单位为 `bps` 。默认值为 8 Mbps 。
     * @param[maxSize] 视频最大宽高，单位为像素。默认值为 `-1` ，表示不做限制，以原始尺寸为准。
     * @param[maxFPS] 视频最大帧率。默认值为 `360` 。
     * @param[enableAudio] 是否启用音频，默认值为关闭。
     * @param[audioCodec] 音频编码。默认值为 AAC 。
     * @param[audioBitRate] 音频码率，单位为 `bps` 。默认值为 128 Kbps 。
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        device: LocalDevice,
        listener: ADBCastMediaListener,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = false,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitRate: Int = 128_000
    ) {
        /* 参数校验 */
        if (!ScrcpyServerHelper.isValid()) {
            val info = "Scrcpy server file invalid or version not set!"
            logger.error(info)
            listener.onConnectError(IllegalStateException(info))
            return
        }


        /* 创建投屏任务 */
        val context = ADBCastContext(
            listener = listener,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            device = device
        )
        synchronized(tasks) {
            if (tasks.containsKey(listener)) {
                val info = "Screen cast task already running!"
                logger.warn(info)
                listener.onConnectError(IllegalStateException(info))
                return
            }

            tasks[listener] = context
        }

        thread {
            try {
                /*
                 * 将服务端复制到设备上。
                 *
                 * 每次启动服务端其文件都会自行删除，因此每次开始屏幕投射前都要复制。
                 */
                val fileReady = device.pushFile(ScrcpyServerHelper.getServerFile()!!, ScrcpyServerHelper.PATH_ON_DEVICE)
                if (!fileReady) {
                    val info = "Push Scrcpy server file to device failed!"
                    logger.error(info)
                    throw IOException(info)
                }
                device.syncFileSystem()


                /* 向 JVM 注册清理线程，当应用进程退出时同步终止 ADB 等外部进程。 */
                val clearThread = Thread {
                    clearContext(context)
                }
                context.clearThread = clearThread
                Runtime.getRuntime().addShutdownHook(clearThread)


                /* 开启 ADB 转发通道 */
                val localPort = findAvailablePort()

                // 如果当前端口已配置转发规则，先将其清除。
                device.stopForward(localPort)
                // SCID 是一个 8 位字符串，用于区分同时启动的多个服务端，此处将端口号作为 SCID 。
                val forwardReady = device.startForward("tcp:$localPort", "localabstract:scrcpy_000$localPort")
                if (!forwardReady) {
                    val info = "Start ADB forward at local port [$localPort] failed!"
                    logger.error(info)
                    throw IOException(info)
                }

                context.forwardPort = localPort


                /* 开启服务端 */
                val cmd = arrayOf(
                    "CLASSPATH=${ScrcpyServerHelper.PATH_ON_DEVICE}",
                    "app_process",
                    "/",
                    "com.genymobile.scrcpy.Server",
                    ScrcpyServerHelper.getServerVersion(),
                    "tunnel_forward=true",
                    "display_id=${displayID}",
                    "video_codec=${videoCodec.cli}",
                    "video_bit_rate=$bitRate",
                    "max_fps=$maxFPS",
                    if (maxSize > 0) "max_size=$maxSize" else "",
                    "audio=$enableAudio",
                    if (enableAudio) "audio_codec=${audioCodec.cli}" else "",
                    if (enableAudio) "audio_bit_rate=$audioBitRate" else "",
                    // 服务端启动后不能动态调整参数，因此默认启用控制通道，由调用者决定是否转发触控事件。
                    "control=true",
                    "log_level=verbose",
                    "scid=000$localPort"
                ).joinToString(" ")
                val serverProcess = device.run(cmd, true)
                context.serverProcess = serverProcess

                // 普通消息输出线程
                thread {
                    serverProcess.inputStream
                        .bufferedReader()
                        .useLines { lines ->
                            lines.forEach { msg -> logger.debug(msg) }
                        }
                }

                // 错误消息输出线程
                thread {
                    serverProcess.errorStream
                        .bufferedReader()
                        .useLines { lines ->
                            lines.forEach { msg ->
                                // 服务端进程被关闭时错误消息会输出空内容，此处将其忽略。
                                if (msg.isBlank()) {
                                    return@thread
                                }

                                val info = "Device report an error! Info:[$msg]"
                                logger.error(info)
                                listener.onConnectError(IllegalStateException(info))
                            }
                        }
                }


                /* 建立视频通道（第一个 TCP 连接） */
                val videoSocket = connectForHandshake(NetUtil.IP_LOOPBACK, localPort, logTag = "Video")
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
                        if (!context.stopped) {
                            listener.onVideoError(e)
                        }
                    }
                }.also { context.videoThread = it }


                /*
                 * 建立音频通道（第二个 TCP 连接）
                 *
                 * 服务端按照客户端连接顺序分配数据通道，分别为：视频 -> 音频 -> 控制，未启用音频时，第二通道为控制通道。
                 */
                if (enableAudio) {
                    val audioSocket = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Audio")
                    audioSocket.tcpNoDelay = true
                    context.audioSocket = audioSocket

                    thread {
                        try {
                            val audioDecoder = AudioDecoder()
                            audioDecoder.init(context)
                            context.audioDecoder = audioDecoder
                            AudioProtocolParser.parse(context)
                        } catch (e: Exception) {
                            if (!context.stopped) {
                                listener.onAudioError(e)
                            }
                        }
                    }.also { context.audioThread = it }
                }


                /* 建立远程控制通道（第三个 TCP 连接） */
                val controlSocket = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Control")
                controlSocket.tcpNoDelay = true
                context.controlSocket = controlSocket

                // 创建输入控制器
                val inputController = InputController(controlSocket.getOutputStream())
                context.inputController = inputController
                listener.onControlReady(inputController)

                // 清除 ADB 转发配置不会打断已建立的 TCP 连接，因此客户端连接完毕后即可调用本方法，不必等到终止投屏时再调用。
                device.stopForward(localPort)
            } catch (e: Exception) {
                /*
                 * 如果任务已经被终止，无需回调释放资源过程中出现的异常。
                 */
                if (!context.stopped) {
                    listener.onConnectError(e)
                }

                clearContext(context)
            }
        }.also { context.taskThread = it }
    }

    /**
     * 停止屏幕投射任务。
     *
     * @param[listener] 事件监听器。
     */
    fun stop(listener: ADBCastMediaListener) {
        synchronized(tasks) {
            tasks.remove(listener)?.let {
                clearContext(it)
            }
        }
    }


    /**
     * 寻找未被占用的本地端口。
     *
     * 从 [forwardPort] 开始依次递增测试端口，若当前端口已被绑定则测试下一个端口，直到可用端口被找到。
     *
     * 端口号达到 [FORWARD_PORT_END] 时回到 [FORWARD_PORT_START] 重新递增尝试。
     *
     * 完整遍历一轮仍未找到可用端口时抛出 [IOException] 。
     *
     * @return 可用的本地端口。
     */
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

        // 如果完整遍历一轮仍未找到可用端口，则抛出异常。
        throw IOException("No available port in [$FORWARD_PORT_START, $FORWARD_PORT_END)!")
    }

    private fun connectForHandshake(
        host: String,
        port: Int,
        retryCount: Int = 50,
        delay: Long = 250L,
        logTag: String? = null
    ): Socket {
        repeat(retryCount) { time ->
            logger.debug("Attempt to connect${if (logTag != null) " [$logTag]" else ""} socket for [${time + 1}] time.")
            try {
                val socket = Socket(host, port)
                // ADB 转发通道开启后连接不会失败，但服务端未就绪读取会出现异常，若能读出握手字节说明服务端就绪。
                val first = DataInputStream(socket.getInputStream()).readByte()
                // 服务端在 Tunnel Forward 模式下会先发送 `0x00` 用于握手。
                if (first == 0x00.toByte()) {
                    return socket
                } else {
                    throw IOException("Unexpected handshake byte! We expect 0x00, but got [0x${first.toString(16)}].")
                }
            } catch (e: Exception) {
                // 若到达预设的最大重试次数，则抛出异常；否则延时片刻并进入下轮循环。
                if (time == retryCount - 1) {
                    logger.warn("Connect${if (logTag != null) " [$logTag]" else ""} socket reach max time.")
                    throw e
                } else {
                    Thread.sleep(delay)
                }
            }
        }

        // 不可达语句，前文要么连接成功返回 Socket ，要么到达最大重试次数抛出异常。
        throw IllegalStateException("Unreachable code!")
    }

    private fun connect(
        host: String,
        port: Int,
        retryCount: Int = 50,
        delay: Long = 250L,
        logTag: String? = null
    ): Socket {
        repeat(retryCount) { time ->
            logger.debug("Attempt to connect${if (logTag != null) " [$logTag]" else ""} socket for [${time + 1}] time.")
            try {
                return Socket(host, port)
            } catch (e: Exception) {
                // 若到达预设的最大重试次数，则抛出异常；否则延时片刻进入下次循环。
                if (time == retryCount - 1) {
                    logger.warn("Connect${if (logTag != null) " [$logTag]" else ""} socket reach max time.")
                    throw e
                } else {
                    Thread.sleep(delay)
                }
            }
        }

        // 不可达语句，前文要么连接成功返回 Socket ，要么到达最大重试次数抛出异常。
        throw IllegalStateException("Unreachable code!")
    }

    // 清理相关资源
    private fun clearContext(context: ADBCastContext) {
        context.apply {
            if (stopped) {
                return
            }

            logger.debug("ClearContext.")
            stopped = true

            forwardPort?.let { device.stopForward(it) }

            runCatching { videoSocket?.close() }
            runCatching { audioSocket?.close() }
            runCatching { controlSocket?.close() }
            CLIUtil.stopProcess(serverProcess)

            // 中断目标线程后需等待目标线程退出再释放解码器，否则 Native 层可能访问中断完成前残留的数据而崩溃。
            videoThread?.let { interruptAndWait(it) }
            audioThread?.let { interruptAndWait(it) }
            taskThread?.let { interruptAndWait(it) }

            videoDecoder?.release()
            audioDecoder?.release()

            runCatching {
                clearThread?.let { Runtime.getRuntime().removeShutdownHook(it) }
            }
            clearThread = null
        }
    }

    /**
     * 中断线程并使调用线程等待其退出。
     *
     * @param[target] 目标线程。
     * @param[timeout] 调用者最大等待时长。
     */
    private fun interruptAndWait(target: Thread, timeout: Long = 2000L) {
        val caller = Thread.currentThread()
        if (target === caller) {
            return
        }

        target.interrupt()
        // 调用 `join()` 前需清除中断标记，防止立刻抛出异常。
        val wasInterrupted = Thread.interrupted()
        try {
            target.join(timeout)
        } catch (_: InterruptedException) {
            // 目标线程中断是我们期望的结果，因此忽略该异常。
        } finally {
            if (wasInterrupted) {
                caller.interrupt()
            }
        }
    }
}
