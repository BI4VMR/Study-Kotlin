package net.bi4vmr.tool.kotlin.util.shell.transfer

import org.jline.terminal.Attributes
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.DataOutputStream
import java.net.Socket
import kotlin.concurrent.thread

/**
 * 终端桥接程序。
 *
 * 与远端主机建立连接，并转发输入/输出到控制台 使用 JLine 处理终端交互指令。
 *
 * 用于取代 `socat` 等外部工具，实现跨平台终端转发。
 *
 * 上行（终端 -> Socket）数据按帧发送：`类型(1 字节) + 长度(4 字节) + 内容`，
 * 类型为 [FRAME_DATA] 时内容为按键数据，为 [FRAME_RESIZE] 时内容为 `"<列数> <行数>"` 。
 * 下行（Socket -> 终端）为原始字节流。
 *
 * 参数：`args[0]` 为本地端口。
 */
object ShellBridge {

    const val FRAME_DATA: Int = 0
    const val FRAME_RESIZE: Int = 1

    @JvmStatic
    fun main(args: Array<String>) {
        val port = args.firstOrNull()?.toIntOrNull()
            ?: error("Usage: ShellBridge <port>")

        val terminal = TerminalBuilder.builder()
            .system(true)
            .jni(true)
            .build()
        val savedAttrs = terminal.enterRawMode()
        // `enterRawMode()` 保留了 ISIG，Ctrl+C 等按键会触发本地信号并结束进程；
        // 关闭 ISIG 后，这些按键将作为普通字节转发给远端 Shell（与 `stty raw` 一致）。
        terminal.attributes = terminal.attributes.apply {
            setLocalFlag(Attributes.LocalFlag.ISIG, false)
        }
        try {
            Socket("127.0.0.1", port).use { socket ->
                val socketOut = DataOutputStream(socket.getOutputStream())

                fun sendFrame(type: Int, data: ByteArray, length: Int = data.size) {
                    synchronized(socketOut) {
                        socketOut.writeByte(type)
                        socketOut.writeInt(length)
                        socketOut.write(data, 0, length)
                        socketOut.flush()
                    }
                }

                fun sendSize() {
                    val size = terminal.size
                    runCatching {
                        sendFrame(FRAME_RESIZE, "${size.columns} ${size.rows}".toByteArray())
                    }
                }

                // 同步终端尺寸：连接时发送一次，窗口大小变化时再次发送。
                sendSize()
                terminal.handle(Terminal.Signal.WINCH) { sendSize() }

                // 终端输入 -> Socket
                thread(isDaemon = true, name = "ShellBridge-Upstream") {
                    val input = terminal.input()
                    val buf = ByteArray(4096)
                    try {
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            sendFrame(FRAME_DATA, buf, n)
                        }
                    } catch (_: Exception) {
                    } finally {
                        runCatching { socket.shutdownOutput() }
                    }
                }

                // Socket -> 终端输出
                val output = terminal.output()
                val socketIn = socket.getInputStream()
                val buf = ByteArray(4096)
                while (true) {
                    val n = socketIn.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    output.flush()
                }
            }
        } finally {
            terminal.attributes = savedAttrs
            terminal.close()
        }
    }
}
