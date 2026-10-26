package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.net.NetUtil
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer.FORWARD_PORT_START
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer.start
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.util.ScrcpyServerHelper
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * ADB 屏幕投射管理（发送端）。
 *
 * 开启 Scrcpy 服务端并将数据转发至 ADBCastReceiver 远程设备，本地不进行音视频解码与显示。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBCastTransfer {

    /**
     * ADB 端口转发的起始端口号。
     */
    private const val FORWARD_PORT_START: Int = 33024 + 1

    /**
     * ADB 端口转发的结束端口号。
     *
     * 端口号达到该值时，回到 [FORWARD_PORT_START] 重新递增尝试。
     */
    private const val FORWARD_PORT_END: Int = 33280


    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBCastTransfer::class.java)

    /**
     * 投屏任务列表。
     */
    private val tasks: MutableSet<ADBCastTransferContext> = mutableSetOf()

    /**
     * 上次使用的 ADB 转发端口。
     *
     * 每次建立新连接时，从当前端口之后一位尝试查找可用端口。
     */
    private val forwardPort: AtomicInteger = AtomicInteger(FORWARD_PORT_START)


    /**
     * 启动屏幕投射转发任务。
     *
     * 连接 ADB 设备，并将设备的音视频数据转发至接收端，同时将接收端的控制数据转发至设备。
     *
     * @param[device] ADB 设备。
     * @param[displayID] 屏幕 ID 。默认为主屏。
     * @param[videoCodec] 视频编码。默认值为 H.264 。
     * @param[bitRate] 视频码率，单位为 `bps` 。默认值为 8 Mbps 。
     * @param[maxSize] 视频最大宽高，单位为像素。默认值为 `-1` ，表示不做限制，以原始尺寸为准。
     * @param[maxFPS] 视频最大帧率。默认值为 `360` 。
     * @param[enableAudio] 是否启用音频，默认值为关闭。
     * @param[audioCodec] 音频编码。默认值为 AAC 。
     * @param[audioBitRate] 音频码率，单位为 `bps` 。默认值为 128 Kbps 。
     * @return 本地监听端口。接收端需要依次建立视频、音频与控制通道；若未启用音频，则只有视频与控制通道。
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        device: LocalDevice,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = false,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitRate: Int = 128_000
    ): Int {
        /* 参数校验 */
        if (!ScrcpyServerHelper.isValid()) {
            throw IllegalStateException("Scrcpy server file invalid or version not set!")
        }

        val context = ADBCastTransferContext(device)
        synchronized(tasks) { tasks.add(context) }

        // 端口由系统分配，接收端依次建立连接，按顺序区分通道：视频 -> 音频（若启用） -> 控制，数据流中无需额外添加标识。
        val remoteServer = ServerSocket(0)
        context.remoteServer = remoteServer

        thread {
            try {
                // 等待接收端连接再开始投屏
                logger.info("Waiting for receiver to connect at port [${remoteServer.localPort}].")
                val remoteVideo = remoteServer.accept().apply { tcpNoDelay = true }
                context.remoteVideoSocket = remoteVideo
                val remoteAudio = if (enableAudio) remoteServer.accept().apply { tcpNoDelay = true } else null
                context.remoteAudioSocket = remoteAudio
                val remoteControl = remoteServer.accept().apply { tcpNoDelay = true }
                context.remoteControlSocket = remoteControl
                logger.info("Receiver connected. Starting scrcpy server.")

                /* 检查 Scrcpy 服务端配置 */
                val serverFile = ScrcpyServerHelper.getServerFile()
                val serverVersion = ScrcpyServerHelper.getServerVersion()
                if (serverFile?.canRead() != true || serverVersion.isBlank()) {
                    val info =
                        "Scrcpy server file or version not set! Path:[${serverFile?.absolutePath}] Version:[$serverVersion]"
                    logger.error(info)
                    throw IOException(info)
                }

                /*
                 * 将服务端复制到设备上。
                 *
                 * 每次启动服务端其文件都会自行删除，因此每次开始屏幕投射前都要复制。
                 */
                val filePushReady = device.pushFile(serverFile, ScrcpyServerHelper.PATH_ON_DEVICE)
                if (!filePushReady) {
                    val info = "Push Scrcpy server file to device failed!"
                    logger.error(info)
                    throw IOException(info)
                }
                device.syncFileSystem()
                logger.debug("Push Scrcpy server file to device [{}] success.", device.getSN())

                // val clearThread = Thread { clearContext(listener, context) }
                // context.clearThread = clearThread
                // Runtime.getRuntime().addShutdownHook(clearThread)


                /* 开启 ADB 转发通道 */
                val localPort = findAvailablePort()
                device.stopForward(localPort)
                val forwardReady = device.startForward("tcp:$localPort", "localabstract:scrcpy_000$localPort")
                if (!forwardReady) {
                    throw IOException("Start ADB forward at local port [$localPort] failed!")
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
                    "display_id=$displayID",
                    "video_codec=${videoCodec.cli}",
                    "video_bit_rate=$bitRate",
                    "max_fps=$maxFPS",
                    if (maxSize > 0) "max_size=$maxSize" else "",
                    "audio=$enableAudio",
                    if (enableAudio) "audio_codec=${audioCodec.cli}" else "",
                    if (enableAudio) "audio_bit_rate=$audioBitRate" else "",
                    "control=true",
                    "log_level=verbose",
                    "scid=000$localPort"
                ).joinToString(" ")
                val serverProcess = device.run(cmd, true)
                context.serverProcess = serverProcess

                thread {
                    serverProcess.inputStream
                        .bufferedReader()
                        .useLines { lines ->
                            lines.forEach { msg -> logger.debug(msg) }
                        }
                }

                thread {
                    serverProcess.errorStream
                        .bufferedReader()
                        .useLines { lines ->
                            lines.forEach { msg ->
                                if (msg.isBlank()) {
                                    return@thread
                                }

                                logger.error("Device report an error! Info:[$msg]")
                                val e = IllegalStateException("Device report an error! Info:[$msg]")
                            }
                        }
                }


                /* 建立视频通道（第一个 TCP 连接） */
                val videoSocket = connectForHandshake(NetUtil.IP_LOOPBACK, localPort, logTag = "Video")
                videoSocket.tcpNoDelay = true
                context.videoSocket = videoSocket

                // 视频通道：设备 -> 接收端，数据原样转发，帧边界由 Scrcpy 协议自带的长度字段确定。
                relay(videoSocket, remoteVideo, context, "Video")
                    .also { context.videoForwardThread = it }


                /*
                 * 建立音频通道（第二个 TCP 连接）
                 *
                 * 服务端按照客户端连接顺序分配数据通道，分别为：视频 -> 音频 -> 控制，未启用音频时，第二通道为控制通道。
                 */
                if (remoteAudio != null) {
                    val audioSocket = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Audio")
                    audioSocket.tcpNoDelay = true
                    context.audioSocket = audioSocket

                    // 音频通道：设备 -> 接收端
                    relay(audioSocket, remoteAudio, context, "Audio")
                        .also { context.audioForwardThread = it }
                }


                /* 建立远程控制通道（第三个 TCP 连接） */
                val controlSocket = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Control")
                controlSocket.tcpNoDelay = true
                context.controlSocket = controlSocket

                // 控制通道：接收端 -> 设备，Scrcpy 控制通道无需反向传递数据。
                relay(remoteControl, controlSocket, context, "Control")
                    .also { context.controlForwardThread = it }


                // 清除 ADB 转发配置不会打断已建立的 TCP 连接，因此客户端连接完毕后即可调用本方法，不必等到终止投屏时再调用。
                device.stopForward(localPort)
                logger.info("Cast task is running.")
            } catch (e: Exception) {
                if (!context.stopped) {
                    logger.error("Cast task failed!", e)
                }
                clearContext(context)
            }
        }.also { context.taskThread = it }

        return remoteServer.localPort
    }

    /**
     * 停止指定投屏任务。
     *
     * @param[port] [start] 方法返回的端口。
     */
    @JvmStatic
    fun stop(port: Int) {
        val context = synchronized(tasks) { tasks.firstOrNull { it.remoteServer?.localPort == port } }
        context?.let { clearContext(it) }
    }

    /**
     * 单向转发数据。
     *
     * 任一方向的连接中断时，释放整个任务的资源。
     */
    private fun relay(from: Socket, to: Socket, context: ADBCastTransferContext, tag: String): Thread {
        return thread {
            try {
                val buffer = ByteArray(64 * 1024)
                val input = from.getInputStream()
                val output = to.getOutputStream()
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) {
                        break
                    }

                    output.write(buffer, 0, read)
                    output.flush()
                }
            } catch (e: Exception) {
                if (!context.stopped) {
                    logger.warn("Relay [$tag] interrupted. Info:[$e]")
                }
            } finally {
                clearContext(context)
            }
        }
    }

    private fun clearContext(context: ADBCastTransferContext) {
        context.apply {
            synchronized(this) {
                if (stopped) {
                    return
                }
                stopped = true
            }

            logger.debug("ClearContext.")
            synchronized(tasks) { tasks.removeAll { it === this } }

            forwardPort?.let { device.stopForward(it) }

            runCatching { remoteServer?.close() }
            runCatching { videoSocket?.close() }
            runCatching { audioSocket?.close() }
            runCatching { controlSocket?.close() }
            runCatching { remoteVideoSocket?.close() }
            runCatching { remoteAudioSocket?.close() }
            runCatching { remoteControlSocket?.close() }
            CLIUtil.stopProcess(serverProcess)
        }
    }

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
