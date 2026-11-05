package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * ADB 设备类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class DeviceType {

    /**
     * USB 连接。
     */
    USB,

    /**
     * TCP/IP 连接。
     */
    NETWORK,

    /**
     * AVD 模拟器。
     */
    EMULATOR;
}
