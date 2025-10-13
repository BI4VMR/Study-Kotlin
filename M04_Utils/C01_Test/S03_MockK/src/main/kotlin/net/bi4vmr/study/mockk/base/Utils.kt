package net.bi4vmr.study.mockk.base

/**
 * 工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Utils {

    fun getCurrentTime(): Long = System.currentTimeMillis()

    @JvmStatic
    fun getURL(): String {
        return "http://192.168.1.1/"
    }
}
