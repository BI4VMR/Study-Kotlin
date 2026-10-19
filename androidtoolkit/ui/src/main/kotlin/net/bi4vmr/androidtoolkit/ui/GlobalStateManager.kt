package net.bi4vmr.androidtoolkit.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.ADBExecutor
import net.bi4vmr.androidtoolkit.core.IDevice
import net.bi4vmr.androidtoolkit.core.LocalExecutor
import net.bi4vmr.androidtoolkit.core.LogicManager

/**
 * 全局状态管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class GlobalStateManager {

    val logicManager = LogicManager()

    /**
     * Flow : ADB 执行器列表。
     */
    val executorsFlow: StateFlow<List<ADBExecutor>> = logicManager.executorsFlow

    private val _selectedExecutorFlow: MutableStateFlow<ADBExecutor> = MutableStateFlow(LocalExecutor)

    /**
     * Flow : 当前选中的 ADB 执行器。
     */
    val selectedExecutorFlow: StateFlow<ADBExecutor> = _selectedExecutorFlow

    private val _selectedDeviceFlow: MutableStateFlow<IDevice?> = MutableStateFlow(null)

    /**
     * Flow : 当前选中的 ADB 设备。
     */
    val selectedDeviceFlow: StateFlow<IDevice?> = _selectedDeviceFlow

    /**
     * 选中执行器，同时清除已选设备。
     */
    fun selectExecutor(executor: ADBExecutor) {
        _selectedExecutorFlow.value = executor
        _selectedDeviceFlow.value = null
    }

    fun selectDevice(device: IDevice?) {
        _selectedDeviceFlow.value = device
    }

    /**
     * 添加远程执行器，成功后自动选中。
     */
    suspend fun addExecutor(host: String): Boolean {
        val result = logicManager.addExecutor(host)
        if (result) {
            executorsFlow.value.lastOrNull()?.let { selectExecutor(it) }
        }
        return result
    }

    fun release() {
        logicManager.release()
    }
}
