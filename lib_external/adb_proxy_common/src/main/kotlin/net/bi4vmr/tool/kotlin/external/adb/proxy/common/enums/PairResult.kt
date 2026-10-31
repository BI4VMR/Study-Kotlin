package net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums

/**
 * 配对模块：配对请求结果。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class PairResult {

    /**
     * 服务端已批准。
     */
    APPROVED,

    /**
     * 服务端已拒绝。
     */
    REJECTED,

    /**
     * 客户端请求终止。
     */
    CANCELLED,

    /**
     * 会话超时。
     */
    EXPIRED,

    /**
     * 会话中止。
     *
     * 例如检测到中间人攻击、当前请求被新的请求覆盖等情况。
     */
    ABORTED;
}
