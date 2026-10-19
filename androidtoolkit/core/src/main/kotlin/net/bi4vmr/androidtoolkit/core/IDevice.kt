package net.bi4vmr.androidtoolkit.core

/**
 * 设备能力公共接口。
 *
 * 定义上层所需的公共能力，本地 ADB 设备和远程 ADB 设备各自进行的实现。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface IDevice {

    /**
     * 获取序列号。
     */
    fun getSN(): String

    /**
     * 获取状态描述。
     */
    fun getStateName(): String

    /**
     * 判断设备是否在线。
     */
    fun isOnline(): Boolean
}
