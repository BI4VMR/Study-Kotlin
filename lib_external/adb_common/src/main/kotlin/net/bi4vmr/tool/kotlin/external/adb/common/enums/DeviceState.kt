package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * ADB 设备状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class DeviceState {

    /**
     * 离线。
     */
    OFFLINE,

    /**
     * 就绪。
     */
    ONLINE,

    /**
     * Recovery。
     */
    RECOVERY,

    /**
     * Bootloader。
     */
    BOOTLOADER;
}
