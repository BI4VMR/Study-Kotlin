package net.bi4vmr.tool.kotlin.external.adb

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.java.common.base.FileUtil
import net.bi4vmr.tool.java.common.base.net.NetUtil
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager.FORWARD_PORT_END
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager.FORWARD_PORT_START
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager.forwardPort
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager.setServerFilePath
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager.setServerVersion
import net.bi4vmr.tool.kotlin.external.adb.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.audio.AudioProtocolParser
import net.bi4vmr.tool.kotlin.external.adb.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.video.VideoProtocolParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * ADB 屏幕投射管理类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBCastManager {

    /**
     * ADB 端口转发的起始端口号。
     */
    private const val FORWARD_PORT_START: Int = 32769

    /**
     * ADB 端口转发的结束端口号。
     *
     * 端口号达到该值时，回到 [FORWARD_PORT_START] 重新递增尝试。
     */
    private const val FORWARD_PORT_END: Int = 40000

    /**
     * 服务端 JAR 文件在 Android 设备上的存放路径。
     */
    private const val SCRCPY_REMOTE_PATH: String = "/data/local/tmp/scrcpy-server"

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBCastManager::class.java)

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
    private val tasks: MutableMap<ADBCastEventListener, ADBCastContext> = mutableMapOf()

    /**
     * 当前 ADB 转发端口。
     *
     * 每次建立新连接时，从当前端口之后一位尝试查找可用端口。
     */
    private val forwardPort: AtomicInteger = AtomicInteger(FORWARD_PORT_START)


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
     * 启动屏幕投射任务。
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        device: ADBDevice,
        listener: ADBCastEventListener,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = true,
        audioCodec: AudioCodec = AudioCodec.FLAC,
        audioBitRate: Int = 128_000,
        remoteControl: Boolean = false,
        debug: Boolean = false
    ) {
        /* 参数校验 */
        if (!serverFile.canRead() || serverVersion.isBlank()) {
            listener.onError(IllegalStateException("Scrcpy server file invalid or version not set!"))
            return
        }

        /* 创建投屏任务 */
        val context = ADBCastContext(device, listener, displayID, videoCodec, audioCodec)
        synchronized(tasks) {
            if (tasks.containsKey(listener)) {
                listener.onError(IllegalStateException("Screen cast task already running!"))
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
                val fileReady = device.pushFile(serverFile, SCRCPY_REMOTE_PATH)
                if (!fileReady) {
                    throw IOException("Push Scrcpy server file to device failed!")
                }
                device.syncFileSystem()


                /* 向 JVM 注册清理线程，当应用进程退出时同步终止 ADB 等外部进程。 */
                val clearThread = Thread {
                    // TODO
                    println("clean up")
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
                    "display_id=${displayID}",
                    "video_codec=${videoCodec.cli}",
                    "video_bit_rate=$bitRate",
                    "max_fps=$maxFPS",
                    if (maxSize > 0) "max_size=$maxSize" else "",
                    "audio=$enableAudio",
                    if (enableAudio) "audio_codec=${audioCodec.cli}" else "",
                    if (enableAudio) "audio_bit_rate=$audioBitRate" else "",
                    "control=$remoteControl",
                    "log_level=verbose",
                    "scid=000$localPort"
                ).joinToString(" ")
                val serverProcess = device.run(cmd, true)
                    ?: throw IOException("Start Scrcpy server process failed!")
                context.serverProcess = serverProcess

                // 普通消息输出线程
                thread {
                    if (debug) {
                        serverProcess.inputStream.copyTo(System.out)
                    } else {
                        // 在子线程消耗服务端进程的输出流，防止阻塞。
                        serverProcess.inputStream.copyTo(OutputStream.nullOutputStream())
                    }
                }

                // 错误消息输出线程
                thread {
                    serverProcess.errorStream
                        .bufferedReader()
                        .useLines { lines ->
                            val msg = lines.joinToString("\n")
                            // 服务端进程被关闭时错误消息会输出空内容，此处将其忽略。
                            if (msg.isBlank()) {
                                return@thread
                            }

                            System.err.println("Device report an error!\n$msg")
                            val e = IllegalStateException("Device report an error!\n$msg")
                            context.serverError = e
                            listener.onError(e)
                        }
                }

                // 建立视频通道（第一个 TCP 连接）
                val videoSocket = connectForHandshake(NetUtil.IP_LOOPBACK, localPort)
                videoSocket.tcpNoDelay = true
                context.videoSocket = videoSocket

                // 循环读取视频通道数据并进行解码
                thread {
                    val videoDecoder = VideoDecoder()
                    videoDecoder.init(context)
                    context.videoDecoder = videoDecoder
                    VideoProtocolParser.parse(context)
                }

                /*
                 * 建立音频通道（第二个 TCP 连接）
                 *
                 * 服务端按照客户端连接顺序分配数据通道，分别为：视频 -> 音频 -> 控制，未启用音频时，第二通道为控制通道。
                 */
                if (enableAudio) {
                    val audioSocket = connect(NetUtil.IP_LOOPBACK, localPort)
                    audioSocket.tcpNoDelay = true
                    context.audioSocket = audioSocket

                    thread {
                        val audioDecoder = AudioDecoder()
                        audioDecoder.init(context)
                        context.audioDecoder = audioDecoder
                        val audioThread = thread {
                            try {
                                AudioProtocolParser.parse(context)
                            } catch (e: Exception) {
                                // 用户主动停止投屏或服务端关闭时，音频流会抛异常，此时不需要上报。
                                if (!Thread.currentThread().isInterrupted) {
                                    listener.onError(e)
                                }
                            }
                        }
                        context.audioThread = audioThread
                    }
                }

                // 建立远程控制通道（第三个 TCP 连接）
                if (remoteControl) {
                    val controlSocket = connect(NetUtil.IP_LOOPBACK, localPort)
                    controlSocket.tcpNoDelay = true
                    context.controlSocket = controlSocket

                    // 创建输入控制器
                    val inputController = InputController(controlSocket.getOutputStream())
                    context.inputController = inputController
                    listener.onControlReady(inputController)
                }

                // 清除 ADB 转发配置不会打断已建立的 TCP 连接，因此客户端连接后就可以调用本方法，不必等到终止投屏时再调用。
                device.stopForward(localPort)
            } catch (e: Exception) {
                /*
                 * 如果任务已经被终止，无需回调释放资源过程中出现的异常。
                 *
                 * 如果服务端已经汇报了异常，无需汇报客户端出现的异常。
                 */
                if (!context.stopped && context.serverError == null) {
                    listener.onError(e)
                }
            } finally {
                // 清理相关资源
                clearContext(context)
            }
        }.also { context.taskThread = it }
    }

    fun stop(listener: ADBCastEventListener) {
        synchronized(tasks) {
            tasks.remove(listener)?.let {
                it.stopped = true
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

    private fun connect(host: String, port: Int, retryCount: Int = 50, delay: Long = 200L): Socket {
        repeat(retryCount) { time ->
            try {
                return Socket(host, port)
            } catch (e: Exception) {
                // 若到达预设的最大重试次数，则抛出异常；否则延时片刻进入下次循环。
                if (time == retryCount - 1) {
                    throw e
                } else {
                    Thread.sleep(delay)
                }
            }
        }

        // 不可达语句，前文要么连接成功返回 Socket ，要么到达最大重试次数抛出异常。
        throw IllegalStateException("Unreachable code!")
    }

    private fun connectForHandshake(host: String, port: Int, retryCount: Int = 50, delay: Long = 200L): Socket {
        repeat(retryCount) { time ->
            try {
                println("attemp for ${time + 1} time.")
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
            videoDecoder?.release()
            audioDecoder?.release()
            runCatching { videoSocket?.close() }
            runCatching { audioSocket?.close() }
            CLIUtil.stopProcess(serverProcess)
            forwardPort?.let { device.stopForward(it) }

            taskThread?.interrupt()
            audioThread?.interrupt()

            clearThread?.let { Runtime.getRuntime().removeShutdownHook(it) }
            clearThread = null
        }
    }
}
