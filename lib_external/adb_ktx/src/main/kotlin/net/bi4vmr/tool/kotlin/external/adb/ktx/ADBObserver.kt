package net.bi4vmr.tool.kotlin.external.adb.ktx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBDeviceListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBServiceListener
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * ADB 服务观察者。
 *
 * ADB 服务相关状态的 Flow 封装，便于 Kotin 工程使用。
 */
class ADBObserver {

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBObserver::class.java)

    /**
     * 是否已初始化。
     */
    @Volatile
    private var initialized: Boolean = false


    /**
     * ADB 服务状态（内部）。
     */
    private val _serviceStateFlow: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.INIT)

    /**
     * ADB 服务状态。
     */
    val serviceStateFlow: StateFlow<ServiceState> = _serviceStateFlow

    /**
     * ADB 服务错误（内部）。
     */
    private val _serviceErrorFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * ADB 服务错误。
     *
     * ADB 服务初始化失败时的错误信息，如果为空表示没有错误出现。
     */
    val serviceErrorStateFlow: StateFlow<Exception?> = _serviceErrorFlow

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
     * 开始监听相关状态。
     */
    @Synchronized
    fun init() {
        logger.debug("Init start.")
        if (initialized) {
            logger.warn("ADBObserver already initialized, ignore.")
            return
        }

        initialized = true

        ADBController.addServiceListener(serviceListener)
        ADBController.addEventListener(eventListener)
        _serviceStateFlow.value = ADBController.getServiceState()
        _devicesFlow.value = ADBController.getDevices()

        logger.debug("Init done.")
    }

    /**
     * 停止监听。
     *
     * 停止监听相关状态，若要恢复监听可以再次调用 [init] 方法。
     */
    fun terminate() {
        logger.debug("Terminate.")
        if (!initialized) {
            logger.warn("Not initialized, ignore.")
            return
        }

        initialized = false

        ADBController.removeEventListener(eventListener)
        ADBController.removeServiceListener(serviceListener)
        _devicesFlow.value = emptyList()
        _serviceErrorFlow.value = null
        _serviceStateFlow.value = ServiceState.INIT
    }


    /**
     * ADB 服务状态监听器实现。
     */
    private val serviceListener = object : ADBServiceListener {

        override fun onStateChange(state: ServiceState) {
            _serviceStateFlow.value = state
        }

        override fun onInitFail(error: Exception?) {
            _serviceErrorFlow.value = error
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
