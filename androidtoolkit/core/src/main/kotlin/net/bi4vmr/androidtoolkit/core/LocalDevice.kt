package net.bi4vmr.androidtoolkit.core

import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice

/**
 * 本地 ADB 设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class LocalDevice(
    val adbDevice: ADBDevice
) : Device {

    override fun getSN(): String = adbDevice.getSN()

    override fun getStateName(): String = adbDevice.getState().name

    override fun isOnline(): Boolean = adbDevice.isOnline()
}
