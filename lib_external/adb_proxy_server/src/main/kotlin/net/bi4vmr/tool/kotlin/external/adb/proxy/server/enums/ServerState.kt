package net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums

import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer

/**
 * 服务端状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ServerState {

    /**
     * 未启动。
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
     *
     * 停止后将回到 [INIT] 状态。
     */
    STOPPING,

    /**
     * 启动错误。
     *
     * 错误信息可从 [ADBProxyServer.stateFlow] 获取。
     */
    ERROR;
}
