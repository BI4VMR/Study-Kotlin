package net.bi4vmr.tool.kotlin.external.adb

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.FileUtil
import net.bi4vmr.tool.java.common.base.net.NetUtil
import net.bi4vmr.tool.kotlin.external.adb.cast.common.ADBCastEventListener
import net.bi4vmr.tool.kotlin.external.adb.cast.common.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.common.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.common.audio.AudioProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.cast.common.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.common.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.common.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.common.video.VideoProtocolParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * ADB 屏幕投射管理类（远程转发）。
 *
 * 接收端调用 [startListening] 监听端口；发送端调用 [startForward] 连接 ADB 设备，并将音视频数据转发至接收端，同时将接收端的
 * 控制数据转发至设备。
 *
 * 发送端与接收端之间建立三条 TCP 连接，分别对应视频、音频与控制通道。每条连接建立后，发送端首先发送会话头：
 *
 * - 会话 ID ： `int64` ，大端。
 * - 通道类型： `uint8` ， `0` 为视频， `1` 为音频， `2` 为控制。
 * - 视频编码名称： `writeUTF` 格式。
 * - 音频编码名称： `writeUTF` 格式，未启用音频时为空字符串。
 *
 * 会话头之后的数据与 Scrcpy 服务端原始数据一致（不含握手字节），控制通道为双向透明转发。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBRemoteCastManager {

    private const val FORWARD_PORT_START: Int = 32769

    private const val FORWARD_PORT_END: Int = 40000

    private const val SCRCPY_REMOTE_PATH: String = "/data/local/tmp/scrcpy-server"

    private const val CHANNEL_VIDEO: Int = 0

    private const val CHANNEL_AUDIO: Int = 1

    private const val CHANNEL_CONTROL: Int = 2

    private val logger: Logger = LoggerFactory.getLogger(ADBRemoteCastManager::class.java)

    private val random: SecureRandom = SecureRandom()

    /**
     * Scrcpy 服务端 JAR 文件路径。
     */
    @Volatile
    private var serverFile: File = FileUtil.FILE_INVALID

    /**
     * Scrcpy 服务端版本。
     */
    @Volatile
    private var serverVersion: String = ""

    /**
     * 发送端任务列表。
     */
    private val tasks: MutableMap<ADBCastEventListener, ADBRemoteCastContext> = mutableMapOf()

    /**
     * 接收端监听任务列表。
     */
    private val receivers: MutableMap<ADBCastEventListener, Receiver> = mutableMapOf()

    private val forwardPort: AtomicInteger = AtomicInteger(FORWARD_PORT_START)

    /**
     * 接收端监听任务。
     */
    private class Receiver(
        val serverSocket: ServerSocket,
        val listener: ADBCastEventListener
    ) {

        val sessions: MutableMap<Long, ADBRemoteReceiveContext> = mutableMapOf()

        @Volatile
        var stopped: Boolean = false
    }


    /**
     * 设置 Scrcpy 服务端文件路径。
     *
     * 未修改官方提供的 JAR 文件名称时，（例如： `scrcpy-server-v4.1` ），可以自动解析版本号，无需调用 [setServerVersion] 方法。
     *
     * @param[path] 服务端文件路径。
     */
    @JvmStatic
    fun setServerFilePath(path: String) {
        logger.debug("SetServerFilePath. Path:[$path]")
        val jarFile = File(path)
        serverFile = jarFile

        val fileName = jarFile.name
        val startOfVersion = fileName.lastIndexOf('v')
        if (startOfVersion != -1 && startOfVersion < fileName.length - 1) {
            val version = fileName.substring(startOfVersion + 1)
            serverVersion = version
            logger.debug("Detected scrcpy server version is [$version].")
        }
    }

    /**
     * 设置 Scrcpy 服务端版本。
     *
     * @param[version] 版本号。
     * @see[setServerFilePath]
     */
    @JvmStatic
    fun setServerVersion(version: String) {
        serverVersion = version
    }

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
    fun startListening(
        listeningHost: String,
        listeningPort: Int,
        listener: ADBCastEventListener
    ): Int {
        val serverSocket = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(listeningHost, listeningPort))
            }
        } catch (e: Exception) {
            listener.onVideoError(e)
            return -1
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

    /**
     * 启动屏幕投射转发任务（发送端）。
     *
     * 连接 ADB 设备，并将设备的音视频数据转发至接收端，同时将接收端的控制数据转发至设备。
     *
     * @param[forwardHost] 接收端地址。
     * @param[forwardPort] 接收端端口。
     * @param[deviceID] ADB 设备序列号。
     * @param[listener] 事件监听器，发送端仅使用其错误回调。
     */
    @JvmStatic
    @JvmOverloads
    fun startForward(
        forwardHost: String,
        forwardPort: Int,
        deviceID: String,
        listener: ADBCastEventListener,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = true,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitRate: Int = 128_000
    ) {
        /* 参数校验 */
        if (!serverFile.canRead() || serverVersion.isBlank()) {
            listener.onVideoError(IllegalStateException("Scrcpy server file invalid or version not set!"))
            return
        }

        val device = ADBController.getDevices().find { it.getSN() == deviceID }
        if (device == null) {
            listener.onVideoError(IllegalStateException("Device [$deviceID] not found!"))
            return
        }

        /* 创建投屏任务 */
        val context = ADBRemoteCastContext(device, listener)
        synchronized(tasks) {
            if (tasks.containsKey(listener)) {
                listener.onVideoError(IllegalStateException("Screen cast task already running!"))
                return
            }

            tasks[listener] = context
        }

        thread {
            try {
                val fileReady = device.pushFile(serverFile, SCRCPY_REMOTE_PATH)
                if (!fileReady) {
                    throw IOException("Push Scrcpy server file to device failed!")
                }
                device.syncFileSystem()

                val clearThread = Thread { clearContext(listener, context) }
                context.clearThread = clearThread
                Runtime.getRuntime().addShutdownHook(clearThread)


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
                    "CLASSPATH=$SCRCPY_REMOTE_PATH",
                    "app_process",
                    "/",
                    "com.genymobile.scrcpy.Server",
                    serverVersion,
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
                    ?: throw IOException("Start Scrcpy server process failed!")
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
                                context.serverError = e
                                listener.onVideoError(e)
                            }
                        }
                }


                /* 连接设备：视频 -> 音频 -> 控制 */
                val deviceVideo = connectForHandshake(NetUtil.IP_LOOPBACK, localPort, logTag = "Video")
                deviceVideo.tcpNoDelay = true
                context.deviceVideoSocket = deviceVideo

                var deviceAudio: Socket? = null
                if (enableAudio) {
                    deviceAudio = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Audio")
                    deviceAudio.tcpNoDelay = true
                    context.deviceAudioSocket = deviceAudio
                }

                val deviceControl = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Control")
                deviceControl.tcpNoDelay = true
                context.deviceControlSocket = deviceControl

                // 客户端连接后即可清除转发配置，不会打断已建立的连接。
                device.stopForward(localPort)


                /* 连接接收端 */
                val sessionID = random.nextLong()
                val audioName = if (enableAudio) audioCodec.name else ""
                val remoteVideo = connectRemote(forwardHost, forwardPort, sessionID, CHANNEL_VIDEO, videoCodec, audioName)
                context.remoteVideoSocket = remoteVideo

                var remoteAudio: Socket? = null
                if (enableAudio) {
                    remoteAudio = connectRemote(forwardHost, forwardPort, sessionID, CHANNEL_AUDIO, videoCodec, audioName)
                    context.remoteAudioSocket = remoteAudio
                }

                val remoteControl = connectRemote(forwardHost, forwardPort, sessionID, CHANNEL_CONTROL, videoCodec, audioName)
                context.remoteControlSocket = remoteControl


                /* 双向转发数据，任一方向结束则终止整个任务。 */
                val onEnd = { clearContext(listener, context) }
                val videoPump = pump("Video", deviceVideo.getInputStream(), remoteVideo.getOutputStream(), context, onEnd)
                if (deviceAudio != null && remoteAudio != null) {
                    pump("Audio", deviceAudio.getInputStream(), remoteAudio.getOutputStream(), context, onEnd)
                }
                pump("ControlUp", remoteControl.getInputStream(), deviceControl.getOutputStream(), context, onEnd)
                pump("ControlDown", deviceControl.getInputStream(), remoteControl.getOutputStream(), context, onEnd)

                videoPump.join()
            } catch (e: Exception) {
                if (!context.stopped && context.serverError == null) {
                    listener.onVideoError(e)
                }
            } finally {
                clearContext(listener, context)
            }
        }.also { context.taskThread = it }
    }

    /**
     * 停止指定监听器关联的发送任务或监听任务。
     */
    @JvmStatic
    fun stop(listener: ADBCastEventListener) {
        synchronized(tasks) { tasks.remove(listener) }?.let { clearContext(listener, it) }
        stopReceiver(listener)
    }

    private fun connectRemote(
        host: String,
        port: Int,
        sessionID: Long,
        channel: Int,
        videoCodec: VideoCodec,
        audioName: String
    ): Socket {
        val socket = connect(host, port, logTag = "Remote-$channel")
        try {
            socket.tcpNoDelay = true
            val out = DataOutputStream(socket.getOutputStream())
            out.writeLong(sessionID)
            out.writeByte(channel)
            out.writeUTF(videoCodec.name)
            out.writeUTF(audioName)
            out.flush()
        } catch (e: Exception) {
            runCatching { socket.close() }
            throw e
        }
        return socket
    }

    private fun pump(
        name: String,
        input: InputStream,
        output: OutputStream,
        context: ADBRemoteCastContext,
        onEnd: () -> Unit
    ): Thread = thread(name = "ADBRemoteCast-$name") {
        try {
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break

                output.write(buffer, 0, count)
                output.flush()
            }
        } catch (e: Exception) {
            if (!context.stopped && context.serverError == null) {
                context.listener.onVideoError(e)
            }
        } finally {
            onEnd()
        }
    }

    private fun clearContext(listener: ADBCastEventListener, context: ADBRemoteCastContext) {
        context.apply {
            if (stopped) {
                return
            }

            logger.debug("ClearContext(Forward).")
            stopped = true
            synchronized(tasks) { tasks.remove(listener) }

            forwardPort?.let { device.stopForward(it) }

            runCatching { deviceVideoSocket?.close() }
            runCatching { deviceAudioSocket?.close() }
            runCatching { deviceControlSocket?.close() }
            runCatching { remoteVideoSocket?.close() }
            runCatching { remoteAudioSocket?.close() }
            runCatching { remoteControlSocket?.close() }
            CLIUtil.stopProcess(serverProcess)

            runCatching {
                clearThread?.let { Runtime.getRuntime().removeShutdownHook(it) }
            }
            clearThread = null
        }
    }

    /* 接收端 */

    private fun stopReceiver(listener: ADBCastEventListener) {
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
