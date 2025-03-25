package net.bi4vmr.study.mockk.callback

import java.io.File

/**
 * 工具类：文件。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
class FileHelper {

    /**
     * 保存文件。
     *
     * @param[callback] 回调接口实现。
     */
    fun saveFile(path: String, callback: FileCallback) {
        try {
            File(path).createNewFile()
            callback.onResult(true, "OK!")
        } catch (e: Exception) {
            callback.onResult(false, "${e.message}")
            System.err.println("Save file failed! Reason:[${e.message}]")
        }
    }
}
