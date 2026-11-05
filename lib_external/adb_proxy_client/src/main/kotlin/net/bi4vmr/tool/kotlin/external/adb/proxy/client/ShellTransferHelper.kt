package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 终端桥接程序文件工具。
 *
 * 管理终端桥接程序（`util:shell_transfer` 模块编译生成的 `shell_transfer.jar`）的路径，
 * 远程 Shell 功能将通过 `java -jar` 启动该程序。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ShellTransferHelper {

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ShellTransferHelper::class.java)

    /**
     * 终端桥接程序 JAR 文件。
     */
    @Volatile
    private var transferFile: File? = null


    /**
     * 获取终端桥接程序文件。
     *
     * @return 桥接程序文件。用户未设置时返回空值。
     */
    @JvmStatic
    fun getTransferFile(): File? = transferFile

    /**
     * 设置终端桥接程序文件。
     *
     * @param[file] 桥接程序文件。
     */
    @JvmStatic
    fun setTransferFile(file: File) {
        logger.debug("SetTransferFile. Path:[${file.absolutePath}]")
        transferFile = file
    }

    /**
     * 设置终端桥接程序文件路径。
     *
     * @param[path] 桥接程序文件路径。
     */
    @JvmStatic
    fun setTransferPath(path: String) = setTransferFile(File(path))

    /**
     * 判断桥接程序文件是否有效。
     *
     * @return `true` 表示文件可读； `false` 表示不可用。
     */
    @JvmStatic
    fun isValid(): Boolean = (transferFile?.canRead() == true)
}
