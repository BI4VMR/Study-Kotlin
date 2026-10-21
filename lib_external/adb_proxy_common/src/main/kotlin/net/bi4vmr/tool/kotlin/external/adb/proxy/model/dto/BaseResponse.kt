package net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto

/**
 * 通用响应消息。
 *
 * 只有泛型参数 `T` 的构造方法将会默认生成一个成功的响应消息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class BaseResponse<T> @JvmOverloads constructor(

    /**
     * 状态码。
     *
     * 默认值 `0` 表示操作成功，其他数值含义视具体协议而定。
     */
    val code: Int = CODE_SUCCESS,

    /**
     * 文本消息。
     */
    val message: String = MESSAGE_SUCCESS,

    /**
     * 数据。
     */
    val data: T? = null
) {

    companion object {

        /**
         * 业务操作成功的默认状态码。
         *
         * 默认值 `0` 表示操作成功，其他数值含义视具体协议而定。
         */
        const val CODE_SUCCESS = 0

        /**
         * 业务操作失败的默认状态码。
         */
        const val CODE_FAILURE = 1

        /**
         * 业务操作成功的默认消息。
         */
        const val MESSAGE_SUCCESS = "OK."

        /**
         * 业务操作失败的默认消息。
         */
        const val MESSAGE_FAILURE = "ERROR!"
    }

    /**
     * 判断当前业务操作是否成功。
     *
     * 该方法仅适用于默认情况：即状态码为 `0` 时视为成功，其他情况视为失败。如果实际业务有其他协定，请不要使用该方法，而是自行实现相关逻辑。
     */
    fun isSuccess(): Boolean {
        return code == CODE_SUCCESS
    }

    /**
     * 判断当前业务操作是否失败。
     *
     * 该方法仅适用于默认情况：即状态码为 `0` 时视为成功，其他情况视为失败。如果实际业务有其他协定，请不要使用该方法，而是自行实现相关逻辑。
     */
    fun isFailure(): Boolean {
        return code != CODE_SUCCESS
    }
}
