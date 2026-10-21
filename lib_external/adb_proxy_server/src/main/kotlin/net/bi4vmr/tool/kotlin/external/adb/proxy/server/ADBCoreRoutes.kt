package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import com.pty4j.PtyProcessBuilder
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBDeviceListener
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.API
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBExecRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.serializer.DisplayInfoSerializer
import org.slf4j.LoggerFactory

/**
 * 服务端核心：基本能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBCoreRoutes : ADBProxyRouteProvider {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBCoreRoutes::class.java)


    override fun Routing.register(jsonTool: Json) {
        get(API.VERSION) {
            call.respondText(Constant.PROTOCOL_VERSION, ContentType.Text.Plain)
        }

        post(API.ADB_DEVICES) {
            val result = onFetchDeviceList(jsonTool)
            call.respondText(result, ContentType.Application.Json)
        }

        post(API.ADB_EXEC_STATUS) {
            val params: ADBExecRequest = try {
                call.receive()
            } catch (e: Exception) {
                call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                return@post
            }

            if (params.sn.isBlank()) {
                call.respondText("Parameter [sn] is required.", status = HttpStatusCode.BadRequest)
                return@post
            }

            val code = onADBExecForStatus(params)

            call.respondText(code.toString(), ContentType.Text.Plain)
        }

        post(API.ADB_EXEC_TEXT) {
            val params: ADBExecRequest = try {
                call.receive()
            } catch (e: Exception) {
                call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                return@post
            }

            if (params.sn.isBlank()) {
                call.respondText("Parameter [sn] is required.", status = HttpStatusCode.BadRequest)
                return@post
            }

            val text = onADBExecForText(params)
            if (text == null) {
                call.respondText("Failed to run command.", status = HttpStatusCode.InternalServerError)
            } else {
                call.respondText(text, ContentType.Text.Plain)
            }
        }

        post(API.ADB_DISPLAYS) {
            val params: ADBExecRequest = try {
                call.receive()
            } catch (e: Exception) {
                call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                return@post
            }

            if (params.sn.isBlank()) {
                call.respondText("Parameter [sn] is required.", status = HttpStatusCode.BadRequest)
                return@post
            }

            val list = ADBCLIUtil.getDisplays(params.sn)
            val result = jsonTool.encodeToString(
                ListSerializer(DisplayInfoSerializer),
                list
            )
            call.respondText(result, ContentType.Application.Json)
        }

        webSocket(API.ADB_EVENTS) {
            onADBEventsWSConnect(this, jsonTool)
        }

        webSocket(API.ADB_SHELL) {
            onADBShellWSConnect(this)
        }
    }


    private fun onFetchDeviceList(jsonTool: Json): String {
        val list = ADBController.getDevices()
            .map { device ->
                RemoteDevice(
                    device.getSN(),
                    device.getState()
                )
            }

        return jsonTool.encodeToString(
            ListSerializer(RemoteDevice.serializer()),
            list
        )
    }

    private suspend fun onADBExecForStatus(params: ADBExecRequest): Int {
        return withContext(Dispatchers.IO) {
            ADBCLIUtil.runForStatus(params.sn, params.command)
        }
    }

    private suspend fun onADBExecForText(params: ADBExecRequest): String? {
        return withContext(Dispatchers.IO) {
            ADBCLIUtil.runForText(params.sn, params.command, params.redirectError, params.charset)
        }
    }

    private suspend fun onADBEventsWSConnect(session: DefaultWebSocketServerSession, jsonTool: Json) {
        // 监听器回调不是挂起函数，因此使用 `trySend()` 将事件投递到发送队列。
        val listener = object : ADBDeviceListener {
            override fun onDeviceListChange(devices: List<ADBDevice>) {
                session.outgoing.trySend(Frame.Text(onFetchDeviceList(jsonTool)))
            }
        }

        // 客户端连接：注册监听器，并推送一次当前设备列表。
        ADBController.addEventListener(listener)
        try {
            session.send(Frame.Text(onFetchDeviceList(jsonTool)))

            // 挂起直到客户端断开（通道关闭或抛出异常）；此处块一旦返回，连接即被关闭。
            session.closeReason.await()
        } finally {
            // 客户端终止（正常关闭、异常断开或服务器停止）：注销监听器。
            ADBController.removeEventListener(listener)
        }
    }

    private suspend fun onADBShellWSConnect(session: DefaultWebSocketServerSession) {
        /* 参数校验 */
        val sn = session.call.request.queryParameters["sn"]
        logger.debug("Receive [${API.ADB_SHELL}] request. SN:[$sn]")
        if (sn.isNullOrBlank()) {
            session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device SN is required."))
            return
        }


        val available = ADBController.getDevices().any { it.getSN() == sn && it.isOnline() }
        if (!available) {
            session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device not ready or not exist."))
            return
        }


        val execName = ADBExecutableHelper.getExecutableName()
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
}
