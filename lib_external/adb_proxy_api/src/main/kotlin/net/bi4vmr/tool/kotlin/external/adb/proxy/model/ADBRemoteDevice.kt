package net.bi4vmr.tool.kotlin.external.adb.proxy.model

import kotlinx.serialization.Serializable
import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.RemoteDeviceState

/**
 * 远程 ADB 设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Serializable
data class ADBRemoteDevice(

    /**
     * 序列号。
     */
    val sn: String,

    /**
     * 状态。
     */
    val state: RemoteDeviceState
) {

    /**
     * 判断设备是否在线。
     *
     * 判断设备是否为 [RemoteDeviceState.ONLINE] 状态。
     *
     * @return `true` 表示在线， `false` 表示其他状态。
     */
    fun isOnline(): Boolean = (state == RemoteDeviceState.ONLINE)
}
