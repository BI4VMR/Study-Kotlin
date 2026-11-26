package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.bi4vmr.tool.java.common.base.io.BaseIOUtil
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException

/**
 * 文件推送管道。
 *
 * 服务端通过 `adb shell cat > <文件路径>` 创建进程，并通过标准输入向目标路径写入数据。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class FilePushPipe(

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
    private val logger = LoggerFactory.getLogger(FilePushPipe::class.java)

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
        pipeProcess = device.run("cat > '$remote'", redirectError = false)
        // 丢弃标准输出，防止阻塞主控线程。
        BaseIOUtil.readAndDrop(pipeProcess.inputStream)
        // 丢弃错误输出，防止阻塞主控线程。
        BaseIOUtil.readAndDrop(pipeProcess.errorStream)
    }

    /**
     * 向设备端写入数据。
     *
     * @param[data] 数据。
     */
    suspend fun write(data: ByteArray) {
        check(!closed) { "File push stream has been closed!" }

        withContext(Dispatchers.IO) {
            pipeProcess.outputStream.let {
                it.write(data)
                it.flush()
            }
        }
    }

    /**
     * 结束任务并等待设备写入剩余数据。
     *
     * @throws[IOException] 写入失败、等待超时或设备端返回非零状态码。
     */
    fun finish() {
        if (closed) {
            return
        }

        closed = true

        try {
            pipeProcess.outputStream.let {
                it.flush()
                it.close()
            }
        } catch (e: IOException) {
            cancel()
            throw e
        }

        val code = pipeProcess.waitFor()
        if (CLIUtil.isFailure(code)) {
            cancel()
            throw IOException("Failed to write file on device! Code:[$code] Remote:[$remote]")
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

        runCatching { pipeProcess.outputStream.close() }
        CLIUtil.stopProcess(pipeProcess)
        logger.debug("Transfer cancelled. Remote:[$remote]")
    }

    override fun close() {
        cancel()
    }
}
