package net.bi4vmr.study.base

/**
 * 模拟系统的USB功能：USB管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object USBManager {

    // 模拟系统API：注册USB状态回调
    fun addUSBStateCallback(cb: USBStateCallback) {
        // Nothing to do.
    }

    // 模拟系统的USB功能：USB状态回调接口。
    interface USBStateCallback {

        // USB设备插入时该接口将被回调
        fun onPlugged()

        // USB设备拔出时该接口将被回调
        fun onUnPlugged()
    }
}
