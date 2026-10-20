package net.bi4vmr.tool.kotlin.external.adb.common.model

import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState

/**
 * ADB 设备公共能力接口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBDevice {

    /*
     * ----- 连接信息 -----
     */

    /**
     * 获取序列号。
     *
     * @return 序列号。
     */
    fun getSN(): String

    /**
     * 获取状态。
     *
     * @return 状态。
     */
    fun getState(): DeviceState

    /**
     * 判断设备是否在线。
     *
     * 判断设备是否为 [DeviceState.ONLINE] 状态。
     *
     * @return `true` 表示在线，可以执行大多数命令， `false` 表示其他状态。
     */
    fun isOnline(): Boolean = (getState() == DeviceState.ONLINE)
}
