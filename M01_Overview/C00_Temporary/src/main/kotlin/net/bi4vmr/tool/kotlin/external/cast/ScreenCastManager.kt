package net.bi4vmr.tool.kotlin.external.cast

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.DisplayInfo
import net.bi4vmr.tool.kotlin.external.cast.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.cast.video.VideoProtocolParser
import java.io.File
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 屏幕投射管理类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ScreenCastManager {

    private const val FORWARD_START_PORT: Int = 32768

    private const val SCRCPY_REMOTE_PATH: String = "/data/local/tmp/scrcpy-server"

    /**
     * 服务端文件路径。
     *
     * 可以下载Scrcpy软件包并获取。
     */
    private var serverFilePath: String = ""

    /**
     * 服务端版本。
     *
     * 应当与服务端文件匹配，格式类似 `4.0` 。
     */
    private var serverVersion: String = ""

    /**
     * 投屏任务列表。
     */
    private val tasks: MutableMap<ScreenCastEventListener, ScreenCastContext> = mutableMapOf()

    /**
     * ADB转发端口。
     */
    private val forwardPort: AtomicInteger = AtomicInteger(FORWARD_START_PORT)

    @JvmStatic
    fun setServerFilePath(path: String) {
        serverFilePath = path
    }

    @JvmStatic
    fun setServerVersion(version: String) {
        serverVersion = version
    }

    @JvmStatic
    @JvmOverloads
    fun start(
        device: ADBDevice,
        listener: ScreenCastEventListener,
        display: DisplayInfo? = null,
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxFPS: Int = 360,
        remoteControl: Boolean = false,
        debug: Boolean = false
    ) {
        /* 参数校验 */
        if (serverFilePath.isBlank() || serverVersion.isBlank()) {
            listener.onError(IllegalStateException("Scrcpy server file path or version is not set!"))
            return
        }


        val context = ScreenCastContext(device, listener, display, videoCodec)

        synchronized(tasks) {
            if (tasks.containsKey(listener)) {
                println("Screen cast task already running!")
                return
            }

            tasks[listener] = context
        }

        thread {
            try {
                /*
                 * 将服务端复制到设备上。
                 *
                 * 每次启动服务端其文件都会自行删除，因此每次屏幕投射前都要复制。
                 */
                val scrcpyServerFile = File(serverFilePath)
                if (!scrcpyServerFile.canRead()) {
                    throw IllegalStateException("Scrcpy server file not exist or readable!")
                }

                val fileReady = device.pushFile(scrcpyServerFile, SCRCPY_REMOTE_PATH)
                if (!fileReady) {
                    throw IOException("Push Scrcpy server file to device failed!")
                }
                device.syncFileSystem()


                /* 开启ADB转发通道 */
                val localPort = forwardPort.getAndIncrement()
                // 如果当前端口已配置转发规则，先将其清除。
                device.stopForward(localPort)
                // SCID是一个8位字符串，用于区分同时启动的多个服务端，此处将端口号作为SCID。
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
                    if (display != null) "display_id=${display.id}" else "",
                    "video_codec=${videoCodec.cli}",
                    "video_bit_rate=$bitRate",
                    "max_fps=$maxFPS",
                    // "max_size=1920",
                    "audio=false",
                    "control=$remoteControl",
                    "log_level=verbose",
                    "scid=000$localPort"
                ).joinToString(" ")
                val serverProcess = device.run(cmd, true)
                    ?: throw IOException("Start Scrcpy server process failed!")
                context.serverProcess = serverProcess

                // 在子线程消耗服务端进程的输出流，防止阻塞。
                thread {
                    serverProcess.inputStream.copyTo(System.out)
                }
                // thread {
                //    val i = serverProcess.waitFor()
                //     println("Server process waiting for $i")
                // }

                // TODO 应当重试来连接
                Thread.sleep(2000)

                val videoSocket = Socket("127.0.0.1", localPort)
                videoSocket.tcpNoDelay = true
                context.videoSocket = videoSocket

                // 清除ADB转发配置不会打断已建立的TCP连接，因此客户端连接后就可以调用本方法，不必等到终止投屏时再调用。
                device.stopForward(localPort)

                Runtime.getRuntime().addShutdownHook(Thread {
                    println("clean up")
                    clearContext(context)
                })

                val videoDecoder = VideoDecoder()
                videoDecoder.init(context)
                context.videoDecoder = videoDecoder
                VideoProtocolParser.parse(context)
            } catch (e: Exception) {
                // 统一通过回调接口上报异常
                listener.onError(e)
            } finally {
                // 清理相关资源
                clearContext(context)
            }
        }.also { context.taskThread = it }
    }

    fun stop(listener: ScreenCastEventListener) {
        synchronized(tasks) {
            tasks.remove(listener)
                ?.let {
                    clearContext(it)
                }
        }
    }

    // 清理相关资源
    private fun clearContext(context: ScreenCastContext) {
        context.apply {
            videoDecoder?.release()
            runCatching { videoSocket?.close() }
            CLIUtil.stopProcess(serverProcess)
            forwardPort?.let { device.stopForward(it) }

            taskThread?.interrupt()

            clearThread?.let { Runtime.getRuntime().removeShutdownHook(it) }
            clearThread = null
        }
    }
}
