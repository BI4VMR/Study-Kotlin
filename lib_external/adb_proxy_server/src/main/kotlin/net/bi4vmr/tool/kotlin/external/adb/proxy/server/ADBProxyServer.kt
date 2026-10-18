package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import com.pty4j.PtyProcessBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ADBEventListener
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.ADBRemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.model.toRemoteState
import org.slf4j.LoggerFactory

/**
 * ADB 代理服务器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBProxyServer(

    /**
     * 监听地址。
     *
     * 默认值为 `0.0.0.0` ，在 Ktor 中该值等效于 `*` 通配符，将会监听所有 IPv4 与 IPv6 地址。
     */
    private val host: String = "0.0.0.0",

    /**
     * 监听端口。
     *
     * 默认值为 [Constant.PORT_SERVER_DEFAULT]。
     */
    private val port: Int = Constant.PORT_SERVER_DEFAULT,
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBProxyServer::class.java)

    /**
     * Ktor 服务器。
     */
    private var server: EmbeddedServer<*, *>? = null

    /**
     * 全局 JSON 工具实例。
     */
    private val jsonTool: Json = Json {
        // 解码时忽略未知字段
        ignoreUnknownKeys = true
        // 编码时对于未指明的字段填入默认值
        encodeDefaults = true
    }

    /**
     * 设备状态监听器实例。
     */
    private val deviceListener: DeviceListener = DeviceListener()

    /**
     * Flow : 设备列表。
     */
    private val devicesFlow: MutableStateFlow<List<ADBDevice>> = MutableStateFlow(emptyList())


    @Synchronized
    fun start() {
        if (server != null) {
            logger.warn("This server is already running, ignored.")
            return
        }

        if (!ADBController.isInitialized()) {
            ADBController.init()
        }
        ADBController.addEventListener(deviceListener)

        server = embeddedServer(CIO, host = host, port = port) {
            configure()
        }.start(wait = false)

        logger.info("Server started on [{}:{}].", host, port)
    }

    @Synchronized
    fun stop() {
        ADBController.removeEventListener(deviceListener)

        server?.stop(500, 2000)
        server = null
    }


    private fun Application.configure() {
        install(WebSockets)

        routing {
            get(Constant.PATH_VERSION) {
                call.respondText(Constant.PROTOCOL_VERSION, ContentType.Text.Plain)
            }

            post(Constant.PATH_ADB_DEVICE_LIST) {
                val result = onFetchDeviceList()
                call.respondText(result, ContentType.Application.Json)
            }

            webSocket(Constant.PATH_ADB_EVENTS) {
                devicesFlow.collect {
                    val frame = Frame.Text(onFetchDeviceList())
                    send(frame)
                }
            }

            webSocket(Constant.PATH_ADB_SHELL) {
                onConnectADBShell(this)
            }
        }
    }

    private fun onFetchDeviceList(): String {
        val list = ADBController.getDevices()
            .map { device ->
                ADBRemoteDevice(
                    device.getSN(),
                    device.getState().toRemoteState()
                )
            }

        return jsonTool.encodeToString(
            ListSerializer(ADBRemoteDevice.serializer()),
            list
        )
    }

    private suspend fun onConnectADBShell(session: DefaultWebSocketServerSession) {
        /* 参数校验 */
        val sn = session.call.request.queryParameters[Constant.PARAM_SN]
        logger.debug("Receive [${Constant.PATH_ADB_SHELL}] request. SN:[$sn]")
        if (sn.isNullOrBlank()) {
            session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device SN is required."))
            return
        }


        val available = ADBController.getDevices().any { it.getSN() == sn && it.isOnline() }
        if (!available) {
            session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device not ready or not exist."))
            return
        }


        val execName = ADBController.getExecutableFile()?.absolutePath ?: "adb"
        val env = HashMap(System.getenv())
        if (env["TERM"] == null) {
            env["TERM"] = "xterm-256color"
        }

        // 使用 `pty4j` 启动 ADB Shell 进程，确保终端控制信息能够被正确处理。
        val process = PtyProcessBuilder(arrayOf(execName, "-s", sn, "shell"))
            .setEnvironment(env)
            .start()


        /* 将 ADB 输出转发到远程设备 */
        val shellToRemoteJob = session.launch {
            try {
                val buffer = ByteArray(4096)
                while (true) {
                    val data = process.inputStream.read(buffer)
                    if (data < 0) {
                        break
                    }

                    val frame = Frame.Binary(true, buffer.copyOf(data))
                    session.send(frame)
                }
            } catch (e: Exception) {
                // TODO
                e.printStackTrace()
            } finally {
                session.close()
            }
        }


        /* 将远程设备输入转发到 ADB 设备 */
        try {
            for (frame in session.incoming) {
                if (frame is Frame.Binary) {
                    val data = frame.readBytes()
                    process.outputStream.apply {
                        write(data)
                        flush()
                    }
                }
            }
        } catch (e: Exception) {
            // TODO
            e.printStackTrace()
        } finally {
            shellToRemoteJob.cancel()
            process.destroy()
        }
    }


    /**
     * ADB 设备状态监听器实现。
     */
    private inner class DeviceListener : ADBEventListener {

        override fun onDeviceListChange(devices: List<ADBDevice>) {
            devicesFlow.value = devices
        }
    }
}
