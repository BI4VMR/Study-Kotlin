package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * ADB 服务状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ServiceState {

    /**
     * 未启动。
     *
     * 未初始化或已停止。
     */
    INIT,

    /**
     * 正在准备。
     */
    PREPARING,

    /**
     * 服务已就绪。
     */
    READY,

    /**
     * 出现错误。
     */
    ERROR;
}
