package net.bi4vmr.study.mockk.callback

/**
 * 回调接口：文件操作。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
interface FileCallback {

    /**
     * 回调方法。
     *
     * @param result  执行结果。
     * @param message 消息。
     */
    fun onResult(result: Boolean, message: String)
}
