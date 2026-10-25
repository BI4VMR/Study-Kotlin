package net.bi4vmr.androidtoolkit.data.file

import net.bi4vmr.tool.java.common.base.FileUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * 文件管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object FileManager {

    /**
     * 用户数据目录名称。
     *
     * TODO TEST
     */
    private const val DIR_DATA: String = ".AndroidToolkit_Exp"

    /**
     * 用户数据目录（临时文件）。
     */
    private const val DIR_TEMP: String = "Temp"

    /**
     * 用户数据目录（配置文件）。
     */
    private const val DIR_CONFIG: String = "Config"

    /**
     * 用户数据目录（数据库文件）。
     */
    private const val DIR_DB: String = "Database"


    fun init() {
        getUserDataDir().mkdir()
        getConfigDir().mkdir()
        getDBDir().mkdir()

        // 先清除临时目录，再重新创建。
        if (getTempDir().exists()) {
            Files.walk(getTempDir().toPath())
                .map(Path::toFile)
                .forEach(File::delete)
        }
        getTempDir().mkdir()
    }

    /**
     * 获取应用程序资源目录。
     *
     * @return 资源目录。
     */
    fun getResourcesDir(): File {
        val path = System.getProperty("compose.application.resources.dir") ?: FileUtil.PATH_INVALID
        return File(path)
    }

    fun getInternalADBExecFile(): File {
        return File(getResourcesDir(), "adb")
    }

    fun getInternalScrcpyServerFile(): File {
        return File(getResourcesDir(), "scrcpy-server-v5.0.1")
    }

    fun getUserDataDir(): File {
        val home = System.getProperty("user.home")
        return File("$home${File.separator}$DIR_DATA")
    }

    fun getTempDir(): File {
        return File(getUserDataDir(), DIR_TEMP)
    }

    fun getConfigDir(): File {
        return File(getUserDataDir(), DIR_CONFIG)
    }

    fun getDBDir(): File {
        return File(getUserDataDir(), DIR_DB)
    }

    fun getConfigFile(): File = File(getConfigDir(), "config.preferences_pb")
}
