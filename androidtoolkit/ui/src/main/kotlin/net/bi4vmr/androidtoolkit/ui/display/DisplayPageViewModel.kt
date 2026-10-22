@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.ui.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.RemoteDevice

/**
 * ViewModel : 显示页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DisplayPageViewModel(
    private val logicManager: LogicManager
) : ViewModel() {

    // 刷新触发器
    private val refreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 屏幕列表。
     *
     * 跟随当前选中的设备，若无选中项则为空列表。
     */
    val displaysFlow: StateFlow<List<DisplayInfo>> =
        logicManager.selectedDeviceFlow
            .combine(refreshTrigger) { device, _ -> device }
            .flatMapLatest { device ->
                flow {
                    val list = when (device) {
                        is LocalDevice -> {
                            device.getDisplays()
                        }
                        is RemoteDevice -> {
                            logicManager.selectedExecutorFlow.value.getDisplays(device.getSN())
                        }
                        else -> emptyList()
                    }
                    emit(list)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Flow : 当前选中的屏幕。
     */
    private val lastSelectedDisplayFlow: MutableStateFlow<DisplayInfo?> = MutableStateFlow(null)

    /**
     * Flow : 当前选中的屏幕。
     */
    val selectedDisplayFlow: StateFlow<DisplayInfo?> =
        combine(lastSelectedDisplayFlow, displaysFlow) { lastSelected, list ->
            if (list.isEmpty()) {
                // 列表为空，清除选中项。
                null
            } else {
                if (lastSelected == null) {
                    // 没有选中项，默认选中第一个。
                    list.first()
                } else {
                    // 选中项非空，检查选中项是否仍在列表中；若不存在则选中第一个。
                    list.find { (it.id == lastSelected.id) && (it.sn == lastSelected.sn) }
                        ?: list.first()
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    /**
     * 选择屏幕。
     */
    fun selectDisplay(display: DisplayInfo) {
        lastSelectedDisplayFlow.value = display
    }

    /**
     * 刷新屏幕列表。
     */
    fun refreshDisplays() {
        refreshTrigger.update { it + 1 }
    }
}
