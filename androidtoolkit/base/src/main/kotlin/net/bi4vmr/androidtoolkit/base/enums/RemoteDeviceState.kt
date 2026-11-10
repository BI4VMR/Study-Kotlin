package net.bi4vmr.androidtoolkit.base.enums

/**
 * ADB 远程设备连接状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class RemoteDeviceState {

    /**
     * 已断开。
     */
    DISCONNECTED,

    /**
     * 正在连接。
     */
    CONNECTING,

    /**
     * 已连接。
     */
    CONNECTED;
}
