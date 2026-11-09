package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * ADB 设备状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class DeviceState {

    /**
     * 就绪。
     */
    ONLINE,

    /**
     * 未认证。
     */
    UNAUTHORIZED,

    /**
     * 正在认证。
     */
    AUTHORIZING,

    /**
     * Bootloader.
     */
    BOOTLOADER,

    /**
     * Fastbootd.
     */
    FASTBOOTD,

    /**
     * Recovery.
     */
    RECOVERY,

    /**
     * Recovery Sideload.
     *
     * 流式写入刷机包，无需将刷机包完整复制到设备上。
     */
    SIDELOAD,

    /**
     * USB 已连接但未开启 ADB 。
     */
    OFFLINE,

    /**
     * USB 连接已断开。
     */
    DISCONNECTED;
}
