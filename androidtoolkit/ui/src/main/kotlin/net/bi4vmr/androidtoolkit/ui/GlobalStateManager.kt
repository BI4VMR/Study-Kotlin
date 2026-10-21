package net.bi4vmr.androidtoolkit.ui

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant

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

    /**
     * Flow : 当前选中的 ADB 执行器。
     */
    val selectedExecutorFlow: StateFlow<ADBExecutor> = logicManager.selectedExecutorFlow

    /**
     * Flow : ADB 设备列表。
     *
     * 跟随当前选中的执行器，若无选中项则为空列表。
     */
    val devicesFlow: StateFlow<List<ADBDevice>> = logicManager.devicesFlow

    /**
     * Flow : 当前选中的 ADB 设备。
     */
    val selectedDeviceFlow: StateFlow<ADBDevice?> = logicManager.selectedDeviceFlow

    fun selectExecutor(executor: ADBExecutor) = logicManager.selectExecutor(executor)

    fun selectDevice(device: ADBDevice?) = logicManager.selectDevice(device)

    suspend fun addRemoteExecutor(host: String, port: Int = Constant.PORT_SERVER_DEFAULT) =
        logicManager.addRemoteExecutor(host, port)

    fun release() {
        logicManager.release()
    }
}
