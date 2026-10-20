package net.bi4vmr.tool.kotlin.external.adb.common.callback

import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice

/**
 * ADB 事件监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBDeviceListener {

    /**
     * 事件：设备列表变更。
     *
     * 设备连接、断开、状态变更均会触发该回调。
     *
     * @param[devices] 设备列表。
     */
    fun onDeviceListChange(devices: List<ADBDevice>) {
        // 默认不作任何操作
    }

    /**
     * 事件：设备已连接。
     *
     * @param[device] 设备。
     */
    fun onDeviceConnect(device: ADBDevice) {
        // 默认不作任何操作
    }

    /**
     * 事件：设备已断开。
     *
     * @param[device] 设备。
     */
    fun onDeviceDisconnect(device: ADBDevice) {
        // 默认不作任何操作
    }

    /**
     * 事件：设备状态变更。
     *
     * @param[device] 设备。
     */
    fun onDeviceStateChange(device: ADBDevice) {
        // 默认不作任何操作
    }
}
