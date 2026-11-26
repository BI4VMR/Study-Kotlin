package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model

import kotlinx.serialization.Serializable

/**
 * 文件传输信令。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class FileTransferEvent<T> @JvmOverloads constructor(

    /**
     * 类型。
     *
     * 对应 [Type] 的 [code]。
     */
    val type: Int,

    /**
     * 数据。
     */
    val data: T? = null
) {

    /**
     * 获取数据。
     *
     * 适用于明确必须返回数据的接口。
     *
     * @return 数据。
     * @throws[IllegalStateException] 如果数据为空值，则抛出异常。
     */
    fun requireData(): T {
        return data ?: throw IllegalStateException("Data is null!")
    }


    /**
     * 事件类型。
     */
    enum class Type(

        /**
         * 编码。
         */
        val code: Int
    ) {

        /**
         * 发送端：已到达文件末尾。
         */
        EOF(1),

        /**
         * 发送端：传输取消。
         */
        CANCEL(2),

        /**
         * 接收端：分片已处理。
         */
        SEGMENT_OK(3),

        /**
         * 接收端：出现错误。
         *
         * 内容为错误信息。
         */
        ERROR(4);


        companion object {

            /**
             * 根据编码获取枚举。
             *
             * @param[code] 编码。
             * @return 事件类型。
             * @throws[IllegalArgumentException] 如果没有对应的事件类型，则抛出异常。
             */
            fun fromCode(code: Int): Type {
                return entries.find { it.code == code }
                    ?: throw IllegalArgumentException("No event type for code: $code")
            }
        }
    }
}
