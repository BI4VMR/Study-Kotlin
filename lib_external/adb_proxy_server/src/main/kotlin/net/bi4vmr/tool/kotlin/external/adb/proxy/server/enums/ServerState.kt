package net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums

/**
 * 服务端状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ServerState {

    /**
     * 未启动。
     *
     * 启动遇到错误或已停止的状态，错误信息可从 [net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer.stateFlow] 获取。
     */
    INIT,

    /**
     * 正在启动。
     */
    STARTING,

    /**
     * 正在运行。
     */
    RUNNING,

    /**
     * 正在停止。
     */
    STOPPING;
}