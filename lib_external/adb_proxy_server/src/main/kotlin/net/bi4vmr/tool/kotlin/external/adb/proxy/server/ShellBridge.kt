package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import org.slf4j.LoggerFactory

/**
 * 将 WebSocket 与 `adb shell` 进程的标准输入输出互相桥接。
 */
internal object ShellBridge {

    private val logger = LoggerFactory.getLogger(ShellBridge::class.java)

    suspend fun run(
        session: DefaultWebSocketSession,
        sn: String,
        term: String,
        cols: Int,
        rows: Int
    ) = withContext(Dispatchers.IO) {
        val adb = ADBController.getExecutableFile()?.absolutePath ?: "adb"
        // adb 仅在本地标准输入为终端时才会启用 raw 模式并转发窗口大小，因此必须为其分配本地 PTY ，
        // 否则方向键、Tab 补全等行编辑功能无法工作。
        val env = HashMap(System.getenv()).apply { put("TERM", term) }
        val process = PtyProcessBuilder(arrayOf(adb, "-s", sn, "shell"))
            .setEnvironment(env)
            .setInitialColumns(cols)
            .setInitialRows(rows)
            .start()
        // 显式设置窗口大小，adb 收到 SIGWINCH 后会将其同步给设备
        process.winSize = WinSize(cols, rows)
        logger.info("Shell session opened: {}", sn)

        val pump = launch {
            val buf = ByteArray(4096)
            val out = process.inputStream
            while (true) {
                val n = out.read(buf)
                if (n < 0) break
                session.send(Frame.Binary(true, buf.copyOf(n)))
            }
            session.close()
        }

        try {
            val stdin = process.outputStream
            for (frame in session.incoming) {
                if (frame is Frame.Binary) {
                    stdin.write(frame.readBytes())
                    stdin.flush()
                }
            }
        } catch (_: Exception) {
            // 连接异常中断，进入清理流程
        } finally {
            process.destroy()
            pump.cancel()
            logger.info("Shell session closed: {}", sn)
        }
    }
}
