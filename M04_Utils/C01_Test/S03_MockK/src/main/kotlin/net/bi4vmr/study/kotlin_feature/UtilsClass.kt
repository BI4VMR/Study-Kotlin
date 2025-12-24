package net.bi4vmr.study.kotlin_feature

/**
 * 工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UtilsClass {

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
