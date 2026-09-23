package net.bi4vmr.tool.kotlin.image.exif.util

import net.bi4vmr.tool.java.common.base.system.OSType
import net.bi4vmr.tool.java.common.base.system.SystemUtil
import java.io.File

/**
 * ExifTool 可执行文件工具。
 *
 * 自动侦测可执行文件位置的相关工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ExifToolExecutableUtil {

    /**
     * 获取当前平台可执行文件名称。
     *
     * @return 文件名称。
     */
    @JvmStatic
    fun getExecutableName(): String {
        return if (SystemUtil.isWindows()) "exiftool.exe" else "exiftool"
    }

    /**
     * 自动侦测可执行文件的位置。
     *
     * 按照以下顺序查找可执行文件：
     *
     * 1. 环境变量 `PATH` 。
     * 2. 常见路径。
     *
     * 通过 CLI 调用本工具时，进程通常能够继承父进程的环境变量；通过 GUI 调用本工具时，系统不会暴露所有环境变量，此时只能遍历常见路径猜测
     * 可执行文件的位置。
     *
     * @return 可执行文件。未找到时将返回空值。
     */
    @JvmStatic
    fun detectExecutable(): File? {
        return findInPath() ?: findInFileSystem()
    }

    /**
     * 在环境变量 `PATH` 中寻找可执行文件。
     *
     * @return 可执行文件。未找到时将返回空值。
     */
    @JvmStatic
    private fun findInPath(): File? {
        val execName = getExecutableName()

        SystemUtil.getPathDirectories()
            .forEach { dir ->
                val test = File(dir, execName)
                if (test.exists()) {
                    return test
                }
            }

        return null
    }

    /**
     * 在常见路径中寻找可执行文件。
     *
     * @return 可执行文件。未找到时将返回空值。
     */
    @JvmStatic
    private fun findInFileSystem(): File? {
        val homePath = System.getProperty("user.home")
        val generalPaths = when (SystemUtil.getOSType()) {
            OSType.WINDOWS -> {
                listOf(
                    "C:/Program Files (x86)/ExifTool/exiftool.exe",
                    "C:/Program Files/ExifTool/exiftool.exe",
                    "C:/Software/ExifTool/exiftool.exe",
                    "C:/Tool/ExifTool/exiftool.exe"
                )
            }
            OSType.MACOS -> {
                listOf(
                    "$homePath/Library/exiftool/exiftool",
                    "/opt/exiftool/exiftool",
                    "/usr/local/bin/exiftool"
                )
            }
            // 未知类型当做 Linux 系统处理
            else -> {
                listOf(
                    "/opt/exiftool/exiftool",
                    "/usr/local/bin/exiftool"
                )
            }
        }

        for (path in generalPaths) {
            val test = File(path)
            if (test.exists()) {
                return test
            }
        }

        return null
    }
}
