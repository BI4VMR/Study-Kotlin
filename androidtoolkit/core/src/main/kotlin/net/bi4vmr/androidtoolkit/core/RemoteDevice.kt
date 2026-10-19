package net.bi4vmr.androidtoolkit.core

import net.bi4vmr.tool.kotlin.external.adb.proxy.model.ADBRemoteDevice

/**
 * 远程 ADB 设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RemoteDevice(
    val remoteDevice: ADBRemoteDevice
) : Device {

    override fun getSN(): String = remoteDevice.sn

    override fun getStateName(): String = remoteDevice.state.name

    override fun isOnline(): Boolean = remoteDevice.isOnline()
}
