package net.bi4vmr.androidtoolkit.ui

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice

/**
 * 全局状态管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class GlobalStateManager {

    /**
     * Flow : 本地 ADB 服务状态。
     */
    val adbStateFlow: StateFlow<Boolean> = ADBObserver.serviceState

    /**
     * Flow : 本地 ADB 设备列表。
     */
    val deviceListFlow: StateFlow<List<ADBDevice>> = ADBObserver.devicesState

    init {
        ADBController.init()
        ADBObserver.init()
    }

    fun release() {

    }
}
