package net.bi4vmr.study.exception

/**
 * 自定义异常。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class CustomException(

    // 错误码
    private val code: Int,

    // 错误信息
    info: String
) : Exception(info) {

    // 获取错误码
    fun getCode(): Int {
        return code
    }
}
