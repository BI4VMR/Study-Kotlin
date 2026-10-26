package net.bi4vmr.tool.kotlin.external.adb.cast.util

import net.bi4vmr.tool.kotlin.external.adb.cast.util.ScrcpyServerHelper.setServerFile
import net.bi4vmr.tool.kotlin.external.adb.cast.util.ScrcpyServerHelper.setServerVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Scrcpy 服务端文件工具。
 *
 * 管理 Scrcpy 服务端 JAR 文件与版本号，供上层模块使用。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ScrcpyServerHelper {

    /**
     * 服务端 JAR 文件在 Android 设备上的存放路径。
     */
    const val PATH_ON_DEVICE: String = "/data/local/tmp/scrcpy-server"


    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ScrcpyServerHelper::class.java)

    /**
     * Scrcpy 服务端 JAR 文件。
     *
     * 可以从 [Scrcpy - Release](https://github.com/Genymobile/scrcpy/releases) 页面下载 Scrcpy 服务端 JAR 包，名称类似
     * `scrcpy-server-v4.1` ，尾部为可变版本号，无 `.jar` 后缀。
     */
    @Volatile
    private var serverFile: File? = null

    /**
     * Scrcpy 服务端版本。
     *
     * 应当与服务端文件匹配，例如：使用 `scrcpy-server-v4.1` 时，应当填写 `4.1` 。
     */
    @Volatile
    private var serverVersion: String = ""


    /**
     * 获取 Scrcpy 服务端文件。
     *
     * @return 服务端文件。用户未设置时返回空值。
     */
    @JvmStatic
    fun getServerFile(): File? = serverFile

    /**
     * 设置 Scrcpy 服务端文件。
     *
     * 未修改官方提供的 JAR 文件名称时，（例如： `scrcpy-server-v4.1` ），可以自动解析版本号，无需调用 [setServerVersion] 方法。
     *
     * @param[file] 服务端文件。
     */
    @JvmStatic
    fun setServerFile(file: File) {
        logger.debug("SetServerFile. Path:[${file.absolutePath}]")
        serverFile = file

        /* 尝试自动设置版本号 */
        val fileName = file.name
        val startOfVersion = fileName.lastIndexOf('v')
        if (startOfVersion != -1 && startOfVersion < fileName.length - 1) {
            val version = fileName.substring(startOfVersion + 1)
            serverVersion = version
            logger.debug("Detected scrcpy server version is [$version].")
        }
    }

    /**
     * 设置 Scrcpy 服务端文件路径。
     *
     * 未修改官方提供的 JAR 文件名称时，（例如： `scrcpy-server-v4.1` ），可以自动解析版本号，无需调用 [setServerVersion] 方法。
     *
     * @param[path] 服务端文件路径。
     */
    @JvmStatic
    fun setServerPath(path: String) = setServerFile(File(path))

    /**
     * 获取 Scrcpy 服务端版本。
     *
     * @return 版本号。未设置时返回空字符串。
     */
    @JvmStatic
    fun getServerVersion(): String = serverVersion

    /**
     * 设置 Scrcpy 服务端版本。
     *
     * 应当与服务端文件匹配，例如：使用 `scrcpy-server-v4.1` 时，应填写 `4.1` 。
     *
     * @param[version] 版本号。
     * @see[setServerFile]
     */
    @JvmStatic
    fun setServerVersion(version: String) {
        serverVersion = version
    }

    /**
     * 判断服务端文件与版本号是否有效。
     *
     * @return `true` 表示文件可读且版本号已设置； `false` 表示不可用。
     */
    @JvmStatic
    fun isValid(): Boolean = (serverFile?.canRead() == true && serverVersion.isNotBlank())
    fun isValid(f:File?): Boolean = (f?.canRead()==true)
}
