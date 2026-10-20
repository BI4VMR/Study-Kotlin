package net.bi4vmr.tool.kotlin.external.adb.proxy.model

import kotlinx.serialization.Serializable
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice

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
    val state: DeviceState
) : ADBDevice {

    /*
     * ----- 连接信息 -----
     */

    /**
     * 获取序列号。
     *
     * @return 序列号。
     */
    override fun getSN(): String = sn

    /**
     * 获取状态。
     *
     * @return 状态。
     */
    override fun getState(): DeviceState = state


    /*
     * ----- 常用命令 -----
     */

}
