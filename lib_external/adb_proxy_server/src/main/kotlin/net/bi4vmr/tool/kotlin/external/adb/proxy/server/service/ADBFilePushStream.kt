package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * ADB 流式文件推送会话。
 *
 * 服务端通过 `adb shell cat > <remote>` 建立设备端文件写入流，接收到的数据直接写入该流，不在代理设备磁盘上缓存完整文件。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBFilePushStream internal constructor(

    /**
     * ADB Shell 进程。
     */
    private val process: Process,

    /**
     * 设备端最终写入的路径。
     */
    val remotePath: String
) : Closeable {

    companion object {

        /**
         * 错误输出最大保留字节数。
         */
        private const val ERROR_BUFFER_LIMIT: Int = 8 * 1024
    }

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBFilePushStream::class.java)

    /**
     * 设备端标准输入流。
     */
    private val output: OutputStream = process.outputStream

    /**
     * 设备端输出的错误信息。
     */
    private val errorBuffer = ByteArrayOutputStream()

    /**
     * 是否已经结束（正常完成或取消）。
     */
    @Volatile
    private var closed: Boolean = false

    /**
     * 输出流读取线程。
     *
     * 持续读取并丢弃 ADB 的输出，防止管道缓冲区写满后阻塞 `cat` 进程。
     */
    private val drainThread = Thread({
        runCatching {
            process.inputStream.use { input ->
                val buffer = ByteArray(4096)
                while (!Thread.currentThread().isInterrupted) {
                    val count = input.read(buffer)
                    if (count < 0) {
                        break
                    }

                    synchronized(errorBuffer) {
                        val remain = ERROR_BUFFER_LIMIT - errorBuffer.size()
                        if (remain > 0) {
                            errorBuffer.write(buffer, 0, minOf(count, remain))
                        }
                    }
                }
            }
        }
    }, "ADBFilePushStream-Drain").apply {
        isDaemon = true
        start()
    }


    /**
     * 向设备端写入数据。
     *
     * 数据写入后立即刷新缓冲区，使数据尽快到达设备端。
     *
     * @param[data] 数据。
     * @throws[IOException] 写入失败。
     */
    @Throws(IOException::class)
    fun write(data: ByteArray) {
        check(!closed) { "File push stream has been closed!" }
        output.write(data)
        output.flush()
    }

    /**
     * 结束写入并等待设备端处理完成。
     *
     * @param[timeoutSeconds] 等待超时时间（秒）。
     * @throws[IOException] 写入失败、等待超时或设备端返回非零状态码。
     */
    @Throws(IOException::class)
    fun finish(timeoutSeconds: Long = 300L) {
        if (closed) {
            return
        }

        try {
            output.flush()
            output.close()
        } catch (e: IOException) {
            cancel()
            throw e
        }

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            cancel()
            throw IOException("Timeout waiting for device to finish writing file! Remote:[$remotePath]")
        }

        closed = true
        drainThread.join(1000L)

        val code = process.exitValue()
        if (code != 0) {
            throw IOException(
                "Failed to write file on device! Code:[$code] Remote:[$remotePath] Message:[${getErrorMessage()}]"
            )
        }
    }

    /**
     * 取消写入并终止设备端进程。
     */
    fun cancel() {
        if (closed) {
            return
        }

        closed = true
        runCatching { output.close() }
        process.destroy()
        if (process.isAlive) {
            process.destroyForcibly()
        }
        logger.debug("File push stream cancelled. Remote:[$remotePath]")
    }

    override fun close() {
        cancel()
    }

    /**
     * 获取设备端输出的错误信息。
     *
     * @return 错误信息。无输出时返回空字符串。
     */
    private fun getErrorMessage(): String {
        return synchronized(errorBuffer) {
            errorBuffer.toString(StandardCharsets.UTF_8.name()).trim()
        }
    }
}
