package net.bi4vmr.tool.kotlin.external.adb.proxy.server.module

import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBDeviceListener
import net.bi4vmr.tool.kotlin.external.adb.common.exceptions.RPCException
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.FileTransferEvent
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBDeviceRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBExecRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBFileTransferRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ADBPropertyRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.serializer.DisplayInfoSerializer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.ADBCoreService
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.FilePullPipe
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.FilePushPipe
import org.slf4j.LoggerFactory

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

    private lateinit var server: ADBProxyServer


    override fun Application.register(server: ADBProxyServer) {
        this@ADBCoreModule.server = server
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

            webSocket(APIs.ADB_FILES_PUSH_FILE) {
                onADBFilePushWSConnect(this)
            }

            webSocket(APIs.ADB_FILES_PULL_FILE) {
                onADBFilePullWSConnect(this)
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

    private suspend fun onADBFilePushWSConnect(session: DefaultWebSocketServerSession) {
        var pipe: FilePushPipe? = null
        try {
            /* 参数校验 */
            // 等待发送端的首帧（指明文件相关属性）
            val fileInfoFrame = session.incoming.receiveCatching().getOrNull() as? Frame.Text
                ?: throw RPCException("Failed to receive file info frame.")
            val request = server.jsonTool.decodeFromString(
                ADBFileTransferRequest.serializer(),
                fileInfoFrame.readText()
            )

            /* 打开 ADB Shell 标准输入通道，向目标文件写入数据。 */
            pipe = service.openFilePushPipe(request)
            pipe.init()

            /* 循环接收发送端的数据帧 */
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        /* 数据帧：文件数据分片 */
                        // 写入设备
                        pipe.write(frame.readBytes())

                        // 向发送端回复确认消息
                        val event = FileTransferEvent<Unit>(FileTransferEvent.Type.SEGMENT_OK.code)
                        session.sendEvent(event)
                    }
                    is Frame.Text -> {
                        /* 文本帧：控制信令 */
                        val event = receiveEvent(frame.readText())
                        val type = FileTransferEvent.Type.fromCode(event.type)
                        when (type) {
                            // 已到达文件末尾
                            FileTransferEvent.Type.EOF -> {
                                pipe.finish()
                                break
                            }
                            // 发送端取消任务
                            FileTransferEvent.Type.CANCEL -> {
                                pipe.cancel()
                                break
                            }
                            else -> Unit
                        }
                    }
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to handle file push request. Reason:[${e.message}]")
            val error = FileTransferEvent(FileTransferEvent.Type.ERROR.code, data = e.message ?: "-")
            session.sendEvent(error)
        } finally {
            pipe?.close()
        }
    }


    private suspend fun onADBFilePullWSConnect(session: DefaultWebSocketServerSession) {
        var pipe: FilePullPipe? = null
        try {
            /* 参数校验 */
            // 等待接收端的首帧（指明文件相关属性）
            val fileInfoFrame = session.incoming.receiveCatching().getOrNull() as? Frame.Text
                ?: throw RPCException("Failed to receive file info frame.")
            val request = server.jsonTool.decodeFromString(
                ADBFileTransferRequest.serializer(),
                fileInfoFrame.readText()
            )

            /* 打开 ADB 标准输出通道，读取源文件数据。 */
            val pullPipe = withContext(Dispatchers.IO) { service.openFilePullPipe(request) }
            pipe = pullPipe
            withContext(Dispatchers.IO) { pullPipe.init() }

            /* 分段读取文件数据并转发给接收端 */
            // 整个读取循环只切换一次 IO 上下文，避免每次阻塞 read 都产生一次调度，
            // 同时保证阻塞读取不会占用 Netty 事件循环。
            val completed = withContext(Dispatchers.IO) {
                val segment = ByteArray(Constants.FILE_SEGMENT_SIZE)
                var filled = 0
                while (true) {
                    val count = pullPipe.read(segment, filled, segment.size - filled)

                    // 到达文件末尾
                    if (count < 0) {
                        // 发送最后一个不足整段的分片；整个文件仅此处发生一次复制。
                        if (filled > 0) {
                            session.send(Frame.Binary(true, segment.copyOf(filled)))

                            // 等待接收端处理完毕当前分片，避免发送端内存被逐渐填满。
                            if (!session.awaitSegmentAck()) {
                                pullPipe.cancel()
                                return@withContext false
                            }
                        }
                        break
                    }

                    if (count == 0) continue

                    filled += count
                    // 缓冲区已满，直接发送同一数组，避免整段复制。
                    // 注意：awaitSegmentAck() 返回前，不得再次读写 segment。
                    if (filled == segment.size) {
                        session.send(Frame.Binary(true, segment))

                        // 等待接收端处理完毕当前分片，避免发送端内存被逐渐填满。
                        if (!session.awaitSegmentAck()) {
                            pullPipe.cancel()
                            return@withContext false
                        }
                        filled = 0
                    }
                }
                true
            }

            // 传输被取消或连接已断开
            if (!completed) return

            // 校验设备端读取结果
            withContext(Dispatchers.IO) { pullPipe.finish() }

            // 通知接收端已到达文件末尾
            val event = FileTransferEvent<Unit>(FileTransferEvent.Type.EOF.code)
            session.sendEvent(event)
        } catch (e: Exception) {
            logger.warn("Failed to handle file pull request. Reason:[${e.message}]")
            val error = FileTransferEvent(FileTransferEvent.Type.ERROR.code, data = e.message ?: "-")
            session.sendEvent(error)
        } finally {
            pipe?.close()
        }
    }


    /**
     * 发送文件传输信令。
     *
     * @param[event] 文件传输信令。
     */
    private suspend inline fun <reified T> DefaultWebSocketServerSession.sendEvent(event: FileTransferEvent<T>) {
        val text = server.jsonTool.encodeToString(
            FileTransferEvent.serializer(serializer<T>()),
            event
        )
        send(Frame.Text(text))
    }

    /**
     * 解析文件传输信令。
     *
     * `data` 保持为 [JsonElement] 类型，调用方根据 `type` 说明可转换为具体类型。
     *
     * @param[text] JSON 文本。
     * @return 文件传输信令。
     */
    private fun receiveEvent(text: String): FileTransferEvent<JsonElement> {
        return server.jsonTool.decodeFromString(
            FileTransferEvent.serializer(JsonElement.serializer()),
            text
        )
    }

    /**
     * 等待接收端回复分片确认信令。
     *
     * @return `true` 表示接收端已处理完当前分片，可以继续发送；`false` 表示接收端已取消或断开连接。
     * @throws[RPCException] 接收端返回错误信令。
     */
    private suspend fun DefaultWebSocketServerSession.awaitSegmentAck(): Boolean {
        while (true) {
            val frame = incoming.receiveCatching().getOrNull() ?: return false
            if (frame !is Frame.Text) continue

            val event = receiveEvent(frame.readText())
            when (FileTransferEvent.Type.fromCode(event.type)) {
                FileTransferEvent.Type.SEGMENT_OK -> return true
                FileTransferEvent.Type.CANCEL -> return false
                FileTransferEvent.Type.ERROR -> throw RPCException(event.requireData().toString())
                else -> Unit
            }
        }
    }
}
