package net.bi4vmr.tool.kotlin.external.adb.proxy

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

    suspend fun run(session: DefaultWebSocketSession, sn: String) = withContext(Dispatchers.IO) {
        val adb = ADBController.getExecutableFile()?.absolutePath ?: "adb"
        // -tt 强制分配伪终端，保证 vi、top 等交互程序可用
        val process = ProcessBuilder(adb, "-s", sn, "shell", "-tt")
            .redirectErrorStream(true)
            .start()
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
