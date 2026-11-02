package net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums

/**
 * 配对模块：配对请求结果。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class PairResult(

    /**
     * 代号。
     *
     * 便于在对等体之间序列化传输。
     */
    val code: Int
) {

    /**
     * 服务端已批准。
     */
    APPROVED(1),

    /**
     * 服务端已拒绝。
     */
    REJECTED(2),

    /**
     * 客户端请求终止。
     */
    CANCELLED(3),

    /**
     * 会话超时。
     */
    EXPIRED(4),

    /**
     * 会话中止。
     *
     * 例如检测到中间人攻击、当前请求被新的请求覆盖等情况。
     */
    ABORTED(5);


    companion object {

        /**
         * 获取代号对应的枚举值。
         *
         * @param[input] 代号。
         * @return 枚举值。若未找到与输入匹配的枚举值则返回空值。
         */
        fun fromCode(input: String): PairResult? {
            val num = input.toIntOrNull() ?: return null

            return entries.find { it.code == num }
        }
    }
}
