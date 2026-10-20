package net.bi4vmr.tool.kotlin.external.adb.common.util

import net.bi4vmr.tool.java.common.base.TextUtil
import net.bi4vmr.tool.java.common.base.system.OSType
import net.bi4vmr.tool.java.common.base.system.SystemUtil
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper.setExecutableFile
import java.io.File

/**
 * ADB 可执行文件工具。
 *
 * 管理 ADB 可执行文件位置，并提供自动侦测功能。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBExecutableHelper {

    /**
     * ADB 可执行文件。
     */
    private var executableFile: File? = detectExecutable()

    /**
     * 是否自动侦测 ADB 可执行文件位置。
     */
    private var autoDetectExecutable: Boolean = true


    /**
     * 获取 ADB 可执行文件。
     *
     * @return 可执行文件。如果当前采用自动侦测但未找到可执行文件，则为空值。
     */
    @JvmStatic
    fun getExecutableFile(): File? = executableFile

    /**
     * 获取 ADB 可执行文件路径。
     *
     * @return 可执行文件路径。如果当前采用自动侦测但未找到可执行文件，则为空值。
     */
    @JvmStatic
    fun getExecutablePath(): String? = executableFile?.absolutePath

    /**
     * 获取 ADB 可执行文件名称。
     *
     * 如果可执行文件存在，则返回完整路径；否则回退到默认名称 `adb` 。可以用于执行命令。
     *
     * @return 可执行文件名称。
     */
    @JvmStatic
    fun getExecutableName(): String = getExecutablePath() ?: "adb"

    /**
     * 设置 ADB 可执行文件。
     *
     * 该方法对已开启的 ADB 进程无效，若要更新这些进程，应当重启它们。
     *
     * @param[file] 可执行文件。若为空值则启用自动侦测。
     */
    @JvmStatic
    @Synchronized
    fun setExecutableFile(file: File? = null) {
        if (file == null) {
            executableFile = detectExecutable()
            autoDetectExecutable = true
        } else {
            executableFile = file
            autoDetectExecutable = false
        }
    }

    /**
     * 设置 ADB 可执行文件路径。
     *
     * @param[path] 可执行文件路径。若为空值则启用自动侦测。
     * @see[setExecutableFile]
     */
    @JvmStatic
    fun setExecutablePath(path: String? = null) {
        if (path == null) {
            setExecutableFile(null)
        } else {
            setExecutableFile(File(path))
        }
    }

    /**
     * 判断是否自动侦测可执行文件位置。
     *
     * @return `true` 表示自动侦测； `false` 表示使用用户指定的路径。
     */
    @JvmStatic
    fun isAutoDetectExecutable(): Boolean = autoDetectExecutable

    /**
     * 自动侦测可执行文件的位置。
     *
     * 按照以下顺序查找可执行文件：
     *
     * 1. 环境变量 `PATH` 。
     * 2. 环境变量 `ANDROID_HOME` 。
     * 3. 常见路径。
     *
     * 通过 CLI 调用本工具时，进程通常能够继承父进程的环境变量；通过 GUI 调用本工具时，系统不会暴露所有环境变量，此时只能遍历常见路径猜测
     * 可执行文件的位置。
     *
     * @return 可执行文件。未找到时将返回空值。
     */
    @JvmStatic
    fun detectExecutable(): File? {
        return findInPath() ?: findInAndroidHome() ?: findInFileSystem()
    }


    /**
     * 获取当前平台可执行文件名称。
     *
     * @return 文件名称。
     */
    private fun getPlatformExecutableName(): String {
        return if (SystemUtil.isWindows()) "adb.exe" else "adb"
    }

    /**
     * 在环境变量 `PATH` 中寻找可执行文件。
     *
     * @return 可执行文件。未找到时将返回空值。
     */
    private fun findInPath(): File? {
        val adbName = getPlatformExecutableName()

        SystemUtil.getPathDirectories()
            .forEach { dir ->
                val test = File(dir, adbName)
                if (test.exists()) {
                    return test
                }
            }

        return null
    }

    /**
     * 在环境变量 `ANDROID_HOME` 中寻找可执行文件。
     *
     * @return 可执行文件。未找到时将返回空值。
     */
    private fun findInAndroidHome(): File? {
        val adbName = getPlatformExecutableName()

        val androidHome = System.getenv("ANDROID_HOME")
        if (TextUtil.isNotBlank(androidHome)) {
            val test = File("$androidHome${File.separator}platform-tools", adbName)
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
    private fun findInFileSystem(): File? {
        val homePath = System.getProperty("user.home")
        val generalPaths = when (SystemUtil.getOSType()) {
            OSType.WINDOWS -> {
                val localAppDataPath = System.getenv("LOCALAPPDATA") ?: "$homePath/AppData/Local"
                listOf(
                    "$localAppDataPath/Android/Sdk/platform-tools/adb.exe",
                    "$homePath/Android/Sdk/platform-tools/adb.exe",
                    "C:/Program Files (x86)/Android/Sdk/platform-tools/adb.exe",
                    "C:/Program Files/Android/Sdk/platform-tools/adb.exe",
                    "C:/Android/Sdk/platform-tools/adb.exe",
                    "D:/Android/Sdk/platform-tools/adb.exe"
                )
            }

            OSType.MACOS -> {
                listOf(
                    "$homePath/Library/Android/sdk/platform-tools/adb",
                    "/opt/homebrew/bin/adb",
                    "/usr/local/bin/adb"
                )
            }
            // 未知类型当做 Linux 系统处理
            else -> {
                listOf(
                    "$homePath/Android/Sdk/platform-tools/adb",
                    "$homePath/.local/share/android-commandlinetools/platform-tools/adb",
                    "$homePath/.local/bin/adb",
                    "/usr/local/bin/adb",
                    "/opt/android-sdk/platform-tools/adb"
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
