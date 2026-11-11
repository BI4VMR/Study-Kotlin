package net.bi4vmr.androidtoolkit.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.base.model.DeviceRecord
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import kotlin.concurrent.thread

/**
 * 伪设备。
 *
 * 已记录在数据库中但未连接的远程设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DummyDevice(
    private val record: DeviceRecord
) : ADBDevice {

    /**
     * Flow：连接状态（内部）。
     */
    private val _connectStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Flow：连接状态。
     *
     * `true` 表示正在连接，`false` 表示未连接。
     *
     * 伪设备仅用于显示未连接与正在连接时的过渡设备，连接成功后会被 `LocalDevice` 取代，因此不需要其他状态。
     */
    val connectStateFlow: StateFlow<Boolean> = _connectStateFlow


    override fun getSN(): String = "${record.host}:${record.port}"

    override fun getState(): DeviceState = DeviceState.DISCONNECTED

    fun connect() {
        thread {
            _connectStateFlow.value = true
            ADBController.connectRemoteDevice(record.host, record.port)
            _connectStateFlow.value = false
        }
    }
}
