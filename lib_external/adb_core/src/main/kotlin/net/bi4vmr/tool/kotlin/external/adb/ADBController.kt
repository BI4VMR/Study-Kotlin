package net.bi4vmr.tool.kotlin.external.adb

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener
import com.android.ddmlib.IDevice
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBDeviceListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBServiceListener
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * ADB 控制器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBController {

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(ADBController::class.java)

    /**
     * DDMLib 回调监听器实例。
     *
     * 仅本工具内部使用，将原始事件处理后转发到 [ADBDeviceListener] 以供调用者监听。
     */
    private val ddmLibListener: DDMLibListener = DDMLibListener()

    /**
     * ADB 设备列表。
     */
    private val deviceList: MutableList<LocalDevice> = mutableListOf()

    /**
     * ADB 服务监听器列表。
     */
    private val serviceListeners: MutableList<ADBServiceListener> = mutableListOf()

    /**
     * ADB 事件监听器列表。
     */
    private val eventListeners: MutableList<ADBDeviceListener> = mutableListOf()

    /**
     * 事件通知线程。
     */
    private var listenerNotifier: ExecutorService = initListenerNotifier()

    /**
     * 事件通知线程名称。
     */
    private const val NOTIFIER_THREAD_NAME = "ADBEventNotifier"

    /**
     * 同步锁。
     */
    private val lock: Any = Any()

    /**
     * ADB 服务初始化状态。
     */
    @Volatile
    private var serviceState: ServiceState = ServiceState.INIT

    /**
     * 是否正在终止。
     *
     * 终止过程中不应当重复初始化。
     */
    private var terminating: Boolean = false


    /*
     * ----- ADB 服务状态管理 -----
     */

    /**
     * 初始化。
     *
     * 进行其他操作之前，应当首先调用本方法。
     *
     * 连接外部 ADB 进程可能耗时较长，建议先注册 [ADBServiceListener] 监听初始化状态，然后在独立线程中调用本方法。
     *
     * @return `true` 表示初始化成功， `false` 表示初始化失败。
     */
    @JvmStatic
    fun init(): Boolean {
        /* 前置检查 */
        synchronized(lock) {
            if (serviceState == ServiceState.PREPARING ||
                serviceState == ServiceState.READY ||
                terminating
            ) {
                logger.warn("Can not init in [$serviceState]!")
                return false
            }

            listenerNotifier = initListenerNotifier()
            notifyServiceInitFailed(null)
            serviceState = ServiceState.PREPARING
            notifyServiceState(ServiceState.PREPARING)
        }

        val adbFile: File? = ADBExecutableHelper.getExecutableFile()
        if (adbFile == null || adbFile.isDirectory || !adbFile.canExecute()) {
            val info = "ADB file is not exist or executable! Path:[$adbFile]"
            logger.error(info)
            synchronized(lock) {
                notifyServiceInitFailed(IllegalStateException(info))
                serviceState = ServiceState.ERROR
                notifyServiceState(ServiceState.ERROR)
                listenerNotifier.shutdown()
            }
            return false
        }

        var error: Exception? = null
        var ready = false
        try {
            // 初始化 DDMLib ，参数表示是否支持连接到应用的 JVM 进行 Debug ，目前不使用相关功能。
            AndroidDebugBridge.init(false)
            // 启动 ADB 的 PC 端进程
            AndroidDebugBridge.createBridge(adbFile.absolutePath, false)
            // 等待当前进程与 ADB 服务进程建立连接
            waitForDDMLibReady()
            ready = true
        } catch (_: InterruptedException) {
            logger.info("DDMLib init has been interrupted!")
            // 用户取消初始化线程不需要上报异常，写回中断状态即可。
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            logger.error("DDMLib init failed!", e)
            error = e
        }

        if (!ready) {
            runCatching {
                AndroidDebugBridge.disconnectBridge()
                AndroidDebugBridge.terminate()
            }

            synchronized(lock) {
                error?.let { notifyServiceInitFailed(it) }
                serviceState = ServiceState.ERROR
                notifyServiceState(ServiceState.ERROR)
                listenerNotifier.shutdown()
            }

            return false
        }

        synchronized(lock) {
            AndroidDebugBridge.addDeviceChangeListener(ddmLibListener)
            getDevicesInner()

            serviceState = ServiceState.READY
            notifyServiceState(ServiceState.READY)
            notifyDeviceListChange(deviceList)
        }

        return true
    }

    /**
     * 等待 DDMLib 连接 ADB 服务进程。
     *
     * @throws[InterruptedException] 等待过程中线程被中断。
     */
    private fun waitForDDMLibReady() {
        val deadline = System.currentTimeMillis() + 10_000L
        var waitMillis = 10L
        while (!isDDMLibReady()) {
            if (System.currentTimeMillis() > deadline) {
                logger.error("Wait for ADB daemon ready timeout!")
                throw IllegalStateException("Wait for ADB daemon ready timeout!")
            }

            Thread.sleep(waitMillis)
            waitMillis = minOf(waitMillis + 10L, 200L)
        }
    }

    // 初始化外部事件通知线程池
    private fun initListenerNotifier(): ExecutorService {
        return Executors.newSingleThreadExecutor { r ->
            Thread(r, NOTIFIER_THREAD_NAME).apply {
                isDaemon = true
            }
        }
    }

    /**
     * 判断 DDMLib 是否已连接 ADB 服务进程。
     *
     * @return `true` 表示 ADB 服务已就绪， `false` 表示 ADB 服务未就绪。
     */
    private fun isDDMLibReady(): Boolean {
        val bridge = AndroidDebugBridge.getBridge()
        return bridge != null &&
                bridge.isConnected &&
                bridge.hasInitialDeviceList()
    }

    /**
     * 获取设备列表。
     *
     * 获取设备列表，并刷新内存缓存。
     */
    private fun getDevicesInner() {
        deviceList.clear()

        val newList = AndroidDebugBridge.getBridge()
            .devices
            .map { LocalDevice(it.state, it) }

        deviceList.addAll(newList)
    }

    /**
     * 获取 ADB 服务状态。
     *
     * @return 服务状态。
     */
    @JvmStatic
    fun getServiceState(): ServiceState = serviceState

    /**
     * ADB 服务是否就绪。
     *
     * @return `true` 表示初始化完成； `false` 表示尚未初始化完成。
     */
    @JvmStatic
    fun isServiceReady(): Boolean = (serviceState == ServiceState.READY)

    /**
     * 终止当前进程与 ADB 服务的连接。
     *
     * 该方法会导致已与 PC 连接的设备断开，若未调用则 DDMLib 内部事件监听线程会使进程无法终止。
     */
    @JvmStatic
    fun terminate() {
        val notifier: ExecutorService
        synchronized(lock) {
            if (!isServiceReady()) {
                return
            }

            terminating = true
            serviceState = ServiceState.INIT

            AndroidDebugBridge.removeDeviceChangeListener(ddmLibListener)
            deviceList.clear()
            notifyDeviceListChange(emptyList())
            notifyServiceState(ServiceState.INIT)
            notifier = listenerNotifier
        }

        try {
            // 停止接收新的任务，已提交的任务继续执行。
            notifier.shutdown()
            // 在通知线程内调用本方法时，不能等待自身，否则会死锁。
            if (Thread.currentThread().name != NOTIFIER_THREAD_NAME) {
                try {
                    if (!notifier.awaitTermination(3, TimeUnit.SECONDS)) {
                        notifier.shutdownNow()
                    }
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    notifier.shutdownNow()
                }
            }

            AndroidDebugBridge.disconnectBridge()
            AndroidDebugBridge.terminate()
        } finally {
            synchronized(lock) {
                terminating = false
            }
        }
    }

    /**
     * 注册 ADB 服务监听器。
     *
     * @param[listener] 监听器实例。
     */
    @JvmStatic
    fun addServiceListener(listener: ADBServiceListener) {
        synchronized(lock) {
            if (!serviceListeners.contains(listener)) {
                serviceListeners.add(listener)
            }
        }
    }

    /**
     * 注消 ADB 服务监听器。
     *
     * @param[listener] 监听器实例。
     */
    @JvmStatic
    fun removeServiceListener(listener: ADBServiceListener) {
        synchronized(lock) {
            serviceListeners.remove(listener)
        }
    }

    /**
     * 通知 ADB 服务状态变更。
     */
    private fun notifyServiceState(state: ServiceState) {
        val listeners = synchronized(lock) {
            serviceListeners.toList()
        }

        listenerNotifier.execute {
            listeners.forEach { listener ->
                runCatching {
                    listener.onStateChange(state)
                }.onFailure {
                    logger.warn("Notify service state [{}] occurred error!", state, it)
                }
            }
        }
    }

    /**
     * 通知 ADB 服务初始化错误。
     */
    private fun notifyServiceInitFailed(error: Exception?) {
        val listeners = synchronized(lock) {
            serviceListeners.toList()
        }

        listenerNotifier.execute {
            listeners.forEach { listener ->
                runCatching {
                    listener.onInitFail(error)
                }.onFailure {
                    logger.warn("Notify service init failed [{}] occurred error!", error, it)
                }
            }
        }
    }


    /*
     * ----- 设备管理 -----
     */

    /**
     * 获取设备列表。
     *
     * 返回本类缓存的设备列表。
     *
     * @return 设备列表。
     */
    @JvmStatic
    fun getDevices(): List<LocalDevice> {
        synchronized(lock) {
            return deviceList.toList()
        }
    }

    /**
     * 连接远程设备。
     *
     * @param[ip] 设备 IP 地址。
     * @throws[IllegalArgumentException] 端口号超出范围。
     * @throws[IllegalStateException] 连接失败。错误信息可通过异常消息读取。
     */
    @JvmStatic
    @JvmOverloads
    fun connectRemoteDevice(ip: String, port: Int = 5555) {
        logger.debug("ConnectRemoteDevice. IP:[{}] Port:[{}]", ip, port)
        if (port !in 1..65535) {
            logger.warn("ConnectRemoteDevice - Port must be in [1, 65535], ignore.")
            throw IllegalArgumentException("Port must be in [1, 65535], ignore.")
        }

        // DDM 库只能发送 ADB Shell 命令，此处直接调用 CLI 连接设备，因此不需要判断 DDM 库的状态。
        val cmd = "adb connect $ip:$port"
        val output = CLIUtil.runForText(cmd, true, StandardCharsets.UTF_8.name())
            ?: throw IllegalStateException("CLI execute failed! CMD:[$cmd]")
        val text = output.trim().lowercase()

        /*
         * 该命令即使连接失败状态码也为成功，因此需要通过输出文本进行判断。
         *
         * 以下返回内容是连接成功的情况：
         *
         * `connected to <IP>:<端口>`：新连接的设备。
         *
         * `already connected to <IP>:<端口>`：设备已连接。
         */
        val failedKeywords = listOf(
            "failed",
            "refused",
            "timeout",
            "timed out",
            "error"
        )
        if (failedKeywords.any { text.contains(it) }) {
            throw IllegalStateException(text)
        }
    }

    /**
     * 断开远程设备。
     *
     * @param[ip] 设备 IP 地址。
     * @return `true` 表示断开成功； `false` 表示断开失败。
     */
    @JvmStatic
    @JvmOverloads
    fun disconnectRemoteDevice(ip: String, port: Int = 5555) {
        logger.debug("DisconnectRemoteDevice. IP:[{}] Port:[{}]", ip, port)
        if (port !in 1..65535) {
            logger.warn("DisconnectRemoteDevice - Port must be in [1, 65535], ignore.")
            return
        }

        val cmd = "adb disconnect $ip:$port"
        val output = CLIUtil.runForText(cmd)
        logger.info("DisconnectRemoteDevice - CLIOutput:[{}]", output)
    }

    /**
     * 注册 ADB 事件监听器。
     *
     * 该监听器不依赖初始化状态，可以在调用 [init] 方法前进行注册，以便接收初始设备列表。
     *
     * @param[listener] 监听器实现。
     */
    @JvmStatic
    fun addEventListener(listener: ADBDeviceListener) {
        synchronized(lock) {
            if (!eventListeners.contains(listener)) {
                eventListeners.add(listener)
            }
        }
    }

    /**
     * 注销 ADB 事件监听器。
     *
     * @param[listener] 监听器实现。
     */
    @JvmStatic
    fun removeEventListener(listener: ADBDeviceListener) {
        synchronized(lock) {
            eventListeners.remove(listener)
        }
    }

    /**
     * 通知设备列表变更。
     *
     * @param[devices] 设备列表。
     */
    private fun notifyDeviceListChange(devices: List<LocalDevice>) {
        var currentDevices: List<LocalDevice> = emptyList()
        var listeners: List<ADBDeviceListener> = emptyList()
        synchronized(lock) {
            currentDevices = devices.toList()
            listeners = eventListeners.toList()
        }

        listenerNotifier.execute {
            listeners.forEach { listener ->
                runCatching {
                    listener.onDeviceListChange(currentDevices)
                }.onFailure {
                    logger.warn("Notify device list change occurred error!", it)
                }
            }
        }
    }

    /**
     * 通知设备已连接。
     *
     * @param[device] 设备。
     */
    private fun notifyDeviceConnect(device: LocalDevice) {
        val listeners = synchronized(lock) {
            eventListeners.toList()
        }

        listenerNotifier.execute {
            listeners.forEach { listener ->
                runCatching {
                    listener.onDeviceConnect(device)
                }.onFailure {
                    logger.warn("Notify device connect occurred error!", it)
                }
            }
        }
    }

    /**
     * 通知设备已断开。
     *
     * @param[device] 设备。
     */
    private fun notifyDeviceDisconnect(device: LocalDevice) {
        val listeners = synchronized(lock) {
            eventListeners.toList()
        }

        listenerNotifier.execute {
            listeners.forEach { listener ->
                runCatching {
                    listener.onDeviceDisconnect(device)
                }.onFailure {
                    logger.warn("Notify device disconnect occurred error!", it)
                }
            }
        }
    }

    /**
     * 通知设备状态变更。
     *
     * @param[device] 设备。
     */
    private fun notifyDeviceStateChange(device: LocalDevice) {
        val listeners = synchronized(lock) {
            eventListeners.toList()
        }

        listenerNotifier.execute {
            listeners.forEach { listener ->
                runCatching {
                    listener.onDeviceStateChange(device)
                }.onFailure {
                    logger.warn("Notify device state change occurred error!", it)
                }
            }
        }
    }


    /**
     * DDMLib 监听器实现。
     *
     * 监听 DDMLib 的设备连接、断开和状态变化事件，并通过 [ADBDeviceListener] 接口通知外部监听者。
     */
    private class DDMLibListener : IDeviceChangeListener {

        override fun deviceConnected(device: IDevice) {
            logger.debug("DDMLib notify device [{}] connected.", device)
            val device = LocalDevice(device.state, device)
            synchronized(lock) {
                // 如果初始化时列表已有该设备，避免重复添加。
                if (deviceList.any { it.ddmDevice == device.ddmDevice }) {
                    return
                }

                deviceList.add(device)
                notifyDeviceListChange(deviceList)
            }
            notifyDeviceConnect(device)
        }

        override fun deviceDisconnected(device: IDevice) {
            logger.debug("DDMLib notify device [{}] disconnected.", device)
            synchronized(lock) {
                val current = deviceList.find { it.ddmDevice == device }
                if (current != null) {
                    deviceList.remove(current)
                    notifyDeviceListChange(deviceList)
                    notifyDeviceDisconnect(current)
                } else {
                    logger.warn("DDMLib notify device [{}] disconnected, but not found in cache!", device)
                }
            }
        }

        override fun deviceChanged(device: IDevice, changeMask: Int) {
            // 设备属性接口兼容性不佳，部分设备无法通过 `getProperty()` 接口获取属性，因此只使用该接口通告设备状态，忽略其他Mask标志位。
            if (changeMask != IDevice.CHANGE_STATE) {
                return
            }

            // 有时设备状态会变为空值，此类事件没有任何含义，因此忽略这些事件。
            if (device.state == null) {
                return
            }

            logger.debug("DDMLib notify device [{}] state changed.", device)

            synchronized(lock) {
                // 寻找 IDevice 属性与当前值对应的 Device
                val current = deviceList.find { it.ddmDevice == device }
                if (current != null) {
                    val index = deviceList.indexOf(current)
                    if (index != -1) {
                        deviceList[index] = current.copy(ddmState = device.state)
                    }
                    notifyDeviceListChange(deviceList)
                    notifyDeviceStateChange(current.copy(ddmState = device.state))
                } else {
                    logger.warn("DDMLib notify device [{}] state changed, but not found in cache!", device)
                }
            }
        }
    }
}
