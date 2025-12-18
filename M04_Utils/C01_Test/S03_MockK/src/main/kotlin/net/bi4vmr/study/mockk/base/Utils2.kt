package net.bi4vmr.study.mockk.base

/**
 * 工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Utils2 {

    companion object {

        fun method(): String {
            return "Method in companion object."
        }

        @JvmStatic
        fun methodStatic(): String {
            return "Static method in companion object."
        }
    }
}
