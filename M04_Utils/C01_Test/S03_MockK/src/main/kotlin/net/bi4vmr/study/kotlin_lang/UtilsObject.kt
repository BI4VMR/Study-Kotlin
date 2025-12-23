package net.bi4vmr.study.kotlin_lang

/**
 * 工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object UtilsObject {

    fun getCurrentTime(): Long = System.currentTimeMillis()

    @JvmStatic
    fun getURL(): String = "http://192.168.1.1/"
}
