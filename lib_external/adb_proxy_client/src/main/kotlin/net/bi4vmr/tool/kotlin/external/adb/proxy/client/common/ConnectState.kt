package net.bi4vmr.tool.kotlin.external.adb.proxy.client.common

/**
 * 连接状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ConnectState {

    /**
     * 连接已断开。
     */
    DISCONNECTED,

    /**
     * 正在连接。
     */
    CONNECTING,

    /**
     * 连接已建立。
     */
    CONNECTED;
}
