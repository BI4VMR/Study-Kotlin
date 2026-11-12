package net.bi4vmr.androidtoolkit.core.model

import net.bi4vmr.androidtoolkit.base.model.DeviceRecord
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice

/**
 * 伪设备。
 *
 * 已记录在数据库中但未连接的远程设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DummyDevice(

    /**
     * 数据库记录。
     */
    private val record: DeviceRecord,

    /**
     * 是否正在连接。
     */
    val connecting: Boolean = false
) : ADBDevice {

    val host: String = record.host

    val port: Int = record.port


    override fun getSN(): String = "${record.host}:${record.port}"

    override fun getState(): DeviceState = DeviceState.DISCONNECTED
}
