package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import net.bi4vmr.tool.java.common.base.io.BaseIOUtil
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException

/**
 * 文件拉取管道。
 *
 * 服务端通过 `adb exec-out cat <文件路径>` 创建进程，并通过标准输出读取文件数据。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class FilePullPipe(

    /**
     * 目标设备。
     */
    private val device: LocalDevice,

    /**
     * 文件路径。
     */
    private val remote: String
) : Closeable {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(FilePullPipe::class.java)

    /**
     * ADB Shell 数据转发进程。
     */
    private lateinit var pipeProcess: Process

    /**
     * 任务是否已经结束。
     */
    @Volatile
    private var closed: Boolean = false


    /**
     * 初始化。
     *
     * 调用其他方法前必须先调用本方法。
     */
    fun init() {
        pipeProcess = device.run("adb exec-out cat '$remote'", redirectError = false)
        // 丢弃错误输出，防止阻塞主控线程。
        BaseIOUtil.readAndDrop(pipeProcess.errorStream)
    }

    /**
     * 读取文件数据。
     *
     * @param[buffer] 数据缓冲区。
     * @param[offset] 缓冲区起始偏移量。
     * @param[length] 本次最多读取的字节数。
     * @return 读取到的字节数；返回 `-1` 表示已到达文件末尾。
     */
    fun read(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Int {
        check(!closed) { "File pull stream has been closed!" }

        return pipeProcess.inputStream.read(buffer, offset, length)
    }

    /**
     * 结束任务并等待设备端读取完毕。
     *
     * @throws[IOException] 读取失败或设备端返回非零状态码。
     */
    fun finish() {
        if (closed) {
            return
        }

        closed = true

        runCatching { pipeProcess.inputStream.close() }

        val code = pipeProcess.waitFor()
        if (CLIUtil.isFailure(code)) {
            throw IOException("Failed to read file on device! Code:[$code] Remote:[$remote]")
        }
    }

    /**
     * 取消任务。
     */
    fun cancel() {
        if (closed) {
            return
        }

        closed = true

        runCatching { pipeProcess.inputStream.close() }
        CLIUtil.stopProcess(pipeProcess)
        logger.debug("Pull cancelled. Remote:[$remote]")
    }

    override fun close() {
        cancel()
    }
}
