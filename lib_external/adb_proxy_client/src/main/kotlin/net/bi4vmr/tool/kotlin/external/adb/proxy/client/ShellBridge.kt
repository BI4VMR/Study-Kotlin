package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.Constant

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Shell 桥接：本地 TCP 连接与服务端 WebSocket 会话之间的字节透传。
 */
class ShellBridge internal constructor(
    private val client: HttpClient,
    private val host: String,
    private val serverPort: Int,
    private val sn: String,
    localPort: Int
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(ShellBridge::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listener = ServerSocket(localPort, 8, InetAddress.getLoopbackAddress())

    // 尽力获取当前终端大小，失败时由服务端使用默认值
    private val cols: Int
    private val rows: Int

    init {
        val size = runCatching {
            val p = ProcessBuilder("sh", "-c", "stty size < /dev/tty").redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readText().trim().split(" ").map { it.toInt() }
        }.getOrNull()
        rows = size?.getOrNull(0) ?: 0
        cols = size?.getOrNull(1) ?: 0
    }

    /**
     * 实际监听的本地端口。
     */
    val port: Int get() = listener.localPort

    init {
        scope.launch {
            while (true) {
                val socket = try {
                    listener.accept()
                } catch (_: Exception) {
                    break
                }
                launch { serve(socket) }
            }
        }
    }

    private val term: String = System.getenv("TERM")
        ?.takeIf { it.isNotBlank() && it != "dumb" }
        ?: "xterm-256color"

    private suspend fun serve(socket: Socket) {
        socket.use {
            try {
                client.webSocket(
                    host = host,
                    port = serverPort,
                    path = "${Constant.PATH_ADB_SHELL}?${Constant.PARAM_SN}=$sn&${Constant.PARAM_TERM}=$term&${Constant.PARAM_COLS}=$cols&${Constant.PARAM_ROWS}=$rows"
                ) {
                    val upstream = launch {
                        val buf = ByteArray(4096)
                        val input = socket.getInputStream()
                        while (true) {
                            val n = withContext(Dispatchers.IO) { input.read(buf) }
                            if (n < 0) break
                            send(Frame.Binary(true, buf.copyOf(n)))
                        }
                        close()
                    }
                    val output = socket.getOutputStream()
                    for (frame in incoming) {
                        if (frame is Frame.Binary) {
                            output.write(frame.readBytes())
                            output.flush()
                        }
                    }
                    upstream.cancel()
                }
            } catch (e: Exception) {
                logger.warn("Shell session ended: {}", e.message)
            }
        }
    }

    override fun close() {
        listener.close()
        scope.cancel()
    }
}
