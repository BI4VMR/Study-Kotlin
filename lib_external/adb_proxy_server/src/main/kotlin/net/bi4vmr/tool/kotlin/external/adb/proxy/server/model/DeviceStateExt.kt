package net.bi4vmr.tool.kotlin.external.adb.proxy.server.model

import net.bi4vmr.tool.kotlin.external.adb.constant.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.RemoteDeviceState

fun DeviceState.toRemoteState(): RemoteDeviceState {
    return when (this) {
        DeviceState.OFFLINE -> RemoteDeviceState.OFFLINE
        DeviceState.ONLINE -> RemoteDeviceState.ONLINE
        DeviceState.RECOVERY -> RemoteDeviceState.RECOVERY
        DeviceState.BOOTLOADER -> RemoteDeviceState.BOOTLOADER
    }
}
