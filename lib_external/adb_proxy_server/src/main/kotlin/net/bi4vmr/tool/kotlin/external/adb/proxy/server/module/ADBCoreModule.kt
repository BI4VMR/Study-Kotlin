package net.bi4vmr.tool.kotlin.external.adb.proxy.server.module

import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBDeviceListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.*
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.serializer.DisplayInfoSerializer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.ADBCoreService
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.ADBFilePushStream
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

/**
 * 服务端核心：基本能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBCoreModule : ADBProxyModule {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBCoreModule::class.java)

    private lateinit var service: ADBCoreService


    override fun Application.register(server: ADBProxyServer) {
        service = ADBCoreService(server)
        service.init()

        routing {
            post(APIs.SERVER_INFO) {
                val response = service.getServerInfo()
                val responseWrapper = BaseResponse(data = response)
                call.respond(responseWrapper)
            }

            post(APIs.ADB_DEVICES) {
                val list = service.getDeviceList()
                val json = server.jsonTool.encodeToString(
                    ListSerializer(RemoteDevice.serializer()),
                    list
                )
                call.respondText(json, ContentType.Application.Json)
            }

            post(APIs.ADB_EXEC_STATUS) {
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

            post(APIs.ADB_EXEC_TEXT) {
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

            post(APIs.ADB_DISPLAYS) {
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
                val result = server.jsonTool.encodeToString(
                    ListSerializer(DisplayInfoSerializer),
                    list
                )
                call.respondText(result, ContentType.Application.Json)
            }

            post(APIs.ADB_PROPERTY) {
                val params: ADBPropertyRequest = try {
                    call.receive()
                } catch (e: Exception) {
                    call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                if (params.sn.isBlank() || params.key.isBlank()) {
                    call.respondText("Parameter [sn] and [key] are required.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val value = withContext(Dispatchers.IO) {
                    ADBCLIUtil.getProperty(params.sn, params.key)
                }
                if (value == null) {
                    call.respondText("Failed to get property.", status = HttpStatusCode.InternalServerError)
                } else {
                    call.respondText(value, ContentType.Text.Plain)
                }
            }

            post(APIs.ADB_DEVICE_NAME) {
                /* 参数校验 */
                val params: ADBDeviceRequest = try {
                    call.receive()
                } catch (_: Exception) {
                    call.respondText("Invalid request body!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val sn = params.sn
                if (sn.isBlank()) {
                    call.respondText("Parameter [sn] is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }


                /* 业务逻辑 */
                val response = withContext(Dispatchers.IO) { service.getDeviceName(sn) }
                call.respond(response)
            }

            post(APIs.ADB_DEVICE_TYPE) {
                /* 参数校验 */
                val params: ADBDeviceRequest = try {
                    call.receive()
                } catch (_: Exception) {
                    call.respondText("Invalid request body!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val sn = params.sn
                if (sn.isBlank()) {
                    call.respondText("Parameter [sn] is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }


                /* 业务逻辑 */
                val response = withContext(Dispatchers.IO) { service.getDeviceType(sn) }
                call.respond(response)
            }

            webSocket(APIs.ADB_EVENTS) {
                onADBEventsWSConnect(this, server.jsonTool)
            }

            webSocket(APIs.ADB_SHELL) {
                onADBShellWSConnect(this)
            }

            webSocket(APIs.ADB_FILE_PUSH) {
                onADBFilePushWSConnect(this, server.jsonTool)
            }

            webSocket(APIs.ADB_FILES_PUSH_FILE) {
                onADBFilePushStreamWSConnect(this, server.jsonTool)
            }
        }
    }

    override fun unregister() {
        service.release()
    }

    private fun onFetchDeviceList(jsonTool: Json): String {
        val list = service.getDeviceList()
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
            ADBCLIUtil.runForText(params.sn, params.command, params.redirectError)
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
        logger.debug("Receive [${APIs.ADB_SHELL}] request. SN:[$sn]")
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
            .setInitialColumns(80)
            .setInitialRows(24)
            .start()


        /* 将 ADB 输出转发到远程设备 */
        val shellToRemoteJob = session.launch {
            try {
                val buffer = ByteArray(4096)
                while (true) {
                    val data = withContext(Dispatchers.IO) { process.inputStream.read(buffer) }
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
                    withContext(Dispatchers.IO) {
                        process.outputStream.apply {
                            write(data)
                            flush()
                        }
                    }
                } else if (frame is Frame.Text) {
                    // 终端尺寸同步，格式：`resize <列数> <行数>`
                    val parts = frame.readText().trim().split(" ")
                    val cols = parts.getOrNull(1)?.toIntOrNull()
                    val rows = parts.getOrNull(2)?.toIntOrNull()
                    if (parts.firstOrNull() == "resize" && cols != null && rows != null && cols > 0 && rows > 0) {
                        runCatching { process.winSize = WinSize(cols, rows) }
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

    private suspend fun onADBFilePushWSConnect(session: DefaultWebSocketServerSession, jsonTool: Json) {
        /* 参数校验 */
        val query = session.call.request.queryParameters
        val sn = query["sn"]
        val remote = query["remote"]
        val name = query["name"]?.let { File(it).name }
        logger.debug("Receive [${APIs.ADB_FILE_PUSH}] request. SN:[$sn] Remote:[$remote] Name:[$name]")
        if (sn.isNullOrBlank() || remote.isNullOrBlank() || name.isNullOrBlank()) {
            session.close(
                CloseReason(
                    CloseReason.Codes.CANNOT_ACCEPT,
                    "Parameter [sn], [remote] and [name] are required."
                )
            )
            return
        }

        val device = ADBController.getDevice(sn)
        if (device == null || !device.isOnline()) {
            session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Device not ready or not exist."))
            return
        }

        fun sendEvent(event: FilePushEvent) {
            session.outgoing.trySend(Frame.Text(jsonTool.encodeToString(FilePushEvent.serializer(), event)))
        }

        // 保持原文件名，使目标路径以 `/` 结尾时的规则与本地推送一致。
        val tempDir = withContext(Dispatchers.IO) { Files.createTempDirectory("adb_proxy_push_").toFile() }
        val tempFile = File(tempDir, name)
        try {
            /* 接收文件数据 */
            var completed = false
            withContext(Dispatchers.IO) {
                tempFile.outputStream().buffered().use { output ->
                    val iterator = session.incoming.iterator()
                    var receiving = true
                    while (receiving && iterator.hasNext()) {
                        val frame = iterator.next()
                        if (frame is Frame.Binary) {
                            output.write(frame.readBytes())
                        } else if (frame is Frame.Text) {
                            when (frame.readText()) {
                                FilePushEvent.CMD_EOF -> {
                                    completed = true
                                    receiving = false
                                }

                                FilePushEvent.CMD_CANCEL -> receiving = false
                            }
                        }
                    }
                }
            }

            if (!completed) {
                sendEvent(FilePushEvent(FilePushEvent.TYPE_END, cancel = true))
                return
            }


            /* 推送至设备 */
            val listener = object : ADBFileTransferListener() {
                override fun onStart(size: Long) {
                    sendEvent(FilePushEvent(FilePushEvent.TYPE_START, total = size))
                }

                override fun onProgress(size: Long, progress: Double) {
                    sendEvent(FilePushEvent(FilePushEvent.TYPE_PROGRESS, size = size, total = tempFile.length()))
                }
            }

            // 推送期间监听客户端的取消指令
            val cancelJob = session.launch {
                for (frame in session.incoming) {
                    if (frame is Frame.Text && frame.readText() == FilePushEvent.CMD_CANCEL) {
                        listener.notifyCancel()
                    }
                }
                // 客户端断开时同样取消推送
                listener.notifyCancel()
            }

            try {
                withContext(Dispatchers.IO) { device.pushFile(tempFile.absolutePath, remote, listener = listener) }
                sendEvent(FilePushEvent(FilePushEvent.TYPE_END, size = tempFile.length(), total = tempFile.length()))
            } catch (e: Exception) {
                if (listener.isCancelled()) {
                    sendEvent(FilePushEvent(FilePushEvent.TYPE_END, cancel = true))
                } else {
                    logger.warn("Failed to push file. Reason:[${e.message}]")
                    sendEvent(FilePushEvent(FilePushEvent.TYPE_ERROR, message = e.message ?: e.javaClass.name))
                }
            } finally {
                cancelJob.cancel()
            }
        } catch (e: Exception) {
            logger.warn("Failed to receive file. Reason:[${e.message}]")
            sendEvent(FilePushEvent(FilePushEvent.TYPE_ERROR, message = e.message ?: e.javaClass.name))
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { tempDir.deleteRecursively() }
            session.close()
        }
    }

    /**
     * 流式文件推送。
     *
     * 与 [onADBFilePushWSConnect] 不同，本方法收到数据后立即写入设备端，不在代理设备磁盘上生成完整临时文件。
     */
    private suspend fun onADBFilePushStreamWSConnect(session: DefaultWebSocketServerSession, jsonTool: Json) {
        /* 参数校验 */
        val query = session.call.request.queryParameters
        val sn = query["sn"]
        val remote = query["remote"]
        val name = query["name"]?.let { File(it).name }
        val total = query["size"]?.toLongOrNull() ?: 0L
        logger.debug("Receive [${APIs.ADB_FILES_PUSH_FILE}] request. SN:[$sn] Remote:[$remote] Name:[$name] Size:[$total]")
        if (sn.isNullOrBlank() || remote.isNullOrBlank() || name.isNullOrBlank()) {
            session.close(
                CloseReason(
                    CloseReason.Codes.CANNOT_ACCEPT,
                    "Parameter [sn], [remote] and [name] are required."
                )
            )
            return
        }

        suspend fun sendEvent(event: FilePushEvent) {
            try {
                val text = jsonTool.encodeToString(FilePushEvent.serializer(), event)
                session.send(Frame.Text(text))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 连接可能已经断开，事件发送失败无需中断资源清理流程。
            }
        }

        var stream: ADBFilePushStream? = null
        try {
            /* 建立设备端写入流 */
            val pushStream = withContext(Dispatchers.IO) {
                service.openFilePushStream(sn, remote, name)
            }
            stream = pushStream
            sendEvent(FilePushEvent(FilePushEvent.TYPE_START, total = total))


            /* 接收数据并立即写入设备 */
            var transferred = 0L
            var completed = false
            var cancelled = false

            while (true) {
                val frame = session.incoming.receiveCatching().getOrNull() ?: break

                if (frame is Frame.Binary) {
                    val data = frame.readBytes()
                    withContext(Dispatchers.IO) { pushStream.write(data) }
                    transferred += data.size
                    sendEvent(FilePushEvent(FilePushEvent.TYPE_PROGRESS, size = transferred, total = total))
                } else if (frame is Frame.Text) {
                    when (frame.readText()) {
                        FilePushEvent.CMD_EOF -> {
                            completed = true
                            break
                        }

                        FilePushEvent.CMD_CANCEL -> {
                            cancelled = true
                            break
                        }
                    }
                }
            }

            if (!completed || cancelled) {
                sendEvent(FilePushEvent(FilePushEvent.TYPE_END, cancel = true))
                return
            }


            /* 等待设备端完成写入 */
            withContext(Dispatchers.IO) { pushStream.finish() }
            sendEvent(FilePushEvent(FilePushEvent.TYPE_END, size = transferred, total = total))
        } catch (e: CancellationException) {
            // 服务端关闭或客户端断开，交由 `finally` 清理资源。
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to push file by stream. Reason:[${e.message}]")
            sendEvent(FilePushEvent(FilePushEvent.TYPE_ERROR, message = e.message ?: e.javaClass.name))
        } finally {
            runCatching { stream?.cancel() }
            runCatching { session.close() }
        }
    }
}
