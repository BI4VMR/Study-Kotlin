package net.bi4vmr.tool.kotlin.external.adb.proxy.constant

import kotlinx.serialization.Serializable

/**
 * 远程 ADB 设备状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
enum class RemoteDeviceState {

    /**
     * 离线。
     */
    OFFLINE,

    /**
     * 就绪。
     */
    ONLINE,

    /**
     * Recovery 。
     */
    RECOVERY,

    /**
     * Bootloader 。
     */
    BOOTLOADER;
}
