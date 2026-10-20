package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * 服务状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ServiceState {

    /**
     * 未就绪。
     */
    IDLE,

    /**
     * 正在准备。
     */
    PREPARING,

    /**
     * 服务已就绪。
     */
    READY;
}
