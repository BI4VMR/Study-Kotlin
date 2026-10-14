package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.FileUtil
import net.bi4vmr.tool.java.common.base.net.NetUtil
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer.FORWARD_PORT_START
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer.LISTENING_PORT_START
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer.setServerFilePath
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer.setServerVersion
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
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
     * 服务端 JAR 文件在 Android 设备上的存放路径。
     */
    private const val SCRCPY_REMOTE_PATH: String = "/data/local/tmp/scrcpy-server"

    /**
     * 本地监听的起始端口号。
     */
    private const val LISTENING_PORT_START: Int = FORWARD_PORT_END + 1

    /**
     * 本地监听的结束端口号。
     *
     * 端口号达到该值时，回到 [LISTENING_PORT_START] 重新递增尝试。
     */
    private const val LISTENING_PORT_END: Int = 33536

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
    private val logger: Logger = LoggerFactory.getLogger(ADBCastTransfer::class.java)

    /**
     * Scrcpy 服务端 JAR 文件路径。
     *
     * 可以从 [Scrcpy - Release](https://github.com/Genymobile/scrcpy/releases) 页面下载 Scrcpy 服务端 JAR 包，名称类似
     * `scrcpy-server-v4.1` ，尾部为可变版本号，无 `.jar` 后缀。
     */
    @Volatile
    private var serverFile: File = FileUtil.FILE_INVALID

    /**
     * Scrcpy 服务端版本。
     *
     * 应当与服务端 JAR 文件匹配，例如：使用 `scrcpy-server-v4.1` 时，应当填写 `4.1` 。
     */
    @Volatile
    private var serverVersion: String = ""

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
     * 上次使用的本地监听端口。
     *
     * 每次建立新连接时，从当前端口之后一位尝试查找可用端口。
     */
    private val listeningPort: AtomicInteger = AtomicInteger(FORWARD_PORT_START)


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

        /* 尝试自动设置版本号 */
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
     * 应当与 JAR 文件匹配，例如：使用 `scrcpy-server-v4.1` 时，应填写 `4.1` 。
     *
     * @param[version] 版本号。
     * @see[setServerFilePath]
     */
    @JvmStatic
    fun setServerVersion(version: String) {
        serverVersion = version
    }

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
     * @return 本地监听端口，接收端需要连接该端口以接收音视频数据。
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        device: ADBDevice,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = false,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitRate: Int = 128_000
    ): Int {
        val context = ADBCastTransferContext(device)
        tasks.add(context)

        thread {
            try {
                /* 参数校验 */
                if (!serverFile.canRead() || serverVersion.isBlank()) {
                    throw IllegalStateException("Scrcpy server file invalid or version not set!")
                }

                val serverSocket = ServerSocket(LISTENING_PORT_START)
                // 等待接收端连接再开始投屏
                val socket = serverSocket.accept()
                // val inputStream = DataOutputStream(socket.getInputStream())
                val outputStream = DataOutputStream(socket.getOutputStream())


                /* 推送服务端 JAR 文件 */
                val fileReady = device.pushFile(serverFile, SCRCPY_REMOTE_PATH)
                if (!fileReady) {
                    throw IOException("Push Scrcpy server file to device failed!")
                }
                device.syncFileSystem()

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
                            }
                        }
                }


                /* 建立视频通道（第一个 TCP 连接） */
                val videoSocket = connectForHandshake(NetUtil.IP_LOOPBACK, localPort, logTag = "Video")
                videoSocket.tcpNoDelay = true
                context.videoSocket = videoSocket

                // 循环读取视频通道数据并转发至接收端
                thread {
                    try {
                        val buffer = ByteArray(1024 * 1024)
                        val inputStream = videoSocket.getInputStream()
                        while (true) {
                            val read = inputStream.read(buffer)
                            if (read < 0) {
                                break
                            }

                            outputStream.writeInt(TYPE_VIDEO_DATA)
                            // outputStream.writeInt(read)
                            outputStream.write(buffer, 0, read)
                        }
                    } catch (e: Exception) {
                        println(e)
                    }
                }.also { context.videoForwardThread = it }


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

                        } catch (e: Exception) {
                            println(e)
                        }
                    }.also { context.audioForwardThread = it }
                }


                /* 建立远程控制通道（第三个 TCP 连接） */
                val controlSocket = connect(NetUtil.IP_LOOPBACK, localPort, logTag = "Control")
                controlSocket.tcpNoDelay = true
                context.controlSocket = controlSocket

                thread {
                    try {

                    } catch (e: Exception) {
                        println(e)
                    }
                }.also { context.controlForwardThread = it }


                // 清除 ADB 转发配置不会打断已建立的 TCP 连接，因此客户端连接完毕后即可调用本方法，不必等到终止投屏时再调用。
                device.stopForward(localPort)
            } catch (e: Exception) {
                println(e)
            }
        }.also { context.taskThread = it }

        return LISTENING_PORT_START
    }

    /**
     * 停止指定监听器关联的发送任务或监听任务。
     */
    @JvmStatic
    fun stop(port: Int) {
        // TODO
    }

    // private fun clearContext(context: ADBCastTransferContext) {
    //     context.apply {
    //         if (stopped) {
    //             return
    //         }
    //
    //         logger.debug("ClearContext(Forward).")
    //         stopped = true
    //         synchronized(tasks) { tasks.remove(listener) }
    //
    //         forwardPort?.let { device.stopForward(it) }
    //
    //         runCatching { deviceVideoSocket?.close() }
    //         runCatching { deviceAudioSocket?.close() }
    //         runCatching { deviceControlSocket?.close() }
    //         runCatching { remoteVideoSocket?.close() }
    //         runCatching { remoteAudioSocket?.close() }
    //         runCatching { remoteControlSocket?.close() }
    //         CLIUtil.stopProcess(serverProcess)
    //
    //         runCatching {
    //             clearThread?.let { Runtime.getRuntime().removeShutdownHook(it) }
    //         }
    //         clearThread = null
    //     }
    // }

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
