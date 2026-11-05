package net.bi4vmr.tool.kotlin.external.adb.ktx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBDeviceListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBServiceListener
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice

/**
 * ADB 相关状态的协程封装。
 */
class ADBObserver {

    /**
     * ADB 服务状态。
     */
    private val _serviceStateFlow: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.INIT)

    /**
     * ADB 服务状态。
     */
    val serviceStateFlow: StateFlow<ServiceState> = _serviceStateFlow

    /**
     * ADB 服务错误。
     */
    private val _initErrorFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * ADB 服务错误。
     */
    val initErrorStateFlow: StateFlow<Exception?> = _initErrorFlow

    /**
     * ADB 设备列表。
     */
    private val _devicesFlow = MutableStateFlow<List<LocalDevice>>(emptyList())

    /**
     * ADB 设备列表。
     */
    val devicesFlow: StateFlow<List<LocalDevice>> = _devicesFlow


    /**
     * 初始化。
     *
     * 开始监听设备变化。
     */
    fun init() {
        ADBController.addServiceListener(serviceListener)
        ADBController.addEventListener(eventListener)
        _serviceStateFlow.value = ADBController.getServiceState()
        _devicesFlow.value = ADBController.getDevices()
    }

    /**
     * 注销。
     *
     * 停止监听设备变化，若要再次监听必须调用 [init] 方法。
     */
    fun terminate() {
        _devicesFlow.value = emptyList()
        _initErrorFlow.value = null
        _serviceStateFlow.value = ServiceState.INIT
        ADBController.removeEventListener(eventListener)
        ADBController.removeServiceListener(serviceListener)
    }


    /**
     * ADB 服务状态监听器实现。
     */
    private val serviceListener = object : ADBServiceListener {

        override fun onStateChange(state: ServiceState) {
            _serviceStateFlow.value = state
        }

        override fun onInitFail(error: Exception?) {
            _initErrorFlow.value = error
        }
    }

    /**
     * ADB 设备列表监听器实现。
     */
    private val eventListener = object : ADBDeviceListener {

        override fun onDeviceListChange(devices: List<ADBDevice>) {
            _devicesFlow.value = ADBController.getDevices()
        }
    }
}
