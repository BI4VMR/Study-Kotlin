@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.ui.generic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo

/**
 * ViewModel：通用页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class GenericPageViewModel(
    val globalStateManager: GlobalStateManager
) : ViewModel() {

    private val logicManager: LogicManager = globalStateManager.logicManager


    // 刷新触发器：全局焦点信息
    private val focusRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow：全局焦点信息。
     *
     * 跟随当前选中的设备，若无选中项则为空列表。
     */
    val focusInfoFlow: StateFlow<FocusInfo> =
        logicManager.selectedDeviceFlow
            .combine(focusRefreshTrigger) { device, _ -> device }
            .flatMapLatest { device ->
                flow {
                    if (device == null) {
                        emit(FocusInfo())
                        return@flow
                    }

                    val info = logicManager.getSelectedExecutor().getGlobalFocusInfo(device.getSN())
                    emit(info)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = FocusInfo()
            )


    /**
     * 刷新全局焦点信息。
     */
    fun refreshFocusInfo() {
        focusRefreshTrigger.update { it + 1 }
    }

    fun getSelectedExecutor(): ADBExecutor {
        return logicManager.getSelectedExecutor()
    }
}
