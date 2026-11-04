package net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums

/**
 * 配对模块：配对状态（客户端）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class PairState {

    /**
     * 服务端未启用认证。
     *
     * 无需配对，可以直接访问业务接口。
     */
    NOT_NEEDED,

    /**
     * 未配对。
     */
    NOT_PAIRED,

    /**
     * 已配对。
     */
    PAIRED;
}
