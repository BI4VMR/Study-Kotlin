@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.ui.generic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel：通用页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class GenericPageViewModel(
    globalStateManager: GlobalStateManager
) : ViewModel() {

    private val logicManager: LogicManager = globalStateManager.logicManager


    /**
     * Flow：全局焦点信息自动刷新间隔（内部）。
     */
    private val _focusInfoRefreshIntervalFlow: MutableStateFlow<Duration?> = MutableStateFlow(null)

    /**
     * Flow：全局焦点信息自动刷新间隔。
     *
     * 空值表示关闭自动刷新。
     */
    val focusInfoRefreshIntervalFlow: StateFlow<Duration?> = _focusInfoRefreshIntervalFlow

    /**
     * Flow：全局焦点信息刷新触发器。
     *
     * 用于触发 `focusInfoFlow` 整体重新订阅，实现手动刷新。
     */
    private val focusRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow：全局焦点信息。
     *
     * 跟随当前选中的设备，若无选中项则为空列表。
     */
    val focusInfoFlow: StateFlow<FocusInfo> =
        combine(
            logicManager.selectedDeviceFlow,
            focusInfoRefreshIntervalFlow,
            focusRefreshTrigger
        ) { device, interval, _ -> device to interval }
            .flatMapLatest { (device, interval) ->
                flow {
                    if (device == null) {
                        emit(FocusInfo())
                        return@flow
                    }

                    while (true) {
                        _focusInfoRefreshStateFlow.value = true
                        try {
                            val info = logicManager.getSelectedExecutor().getGlobalFocusInfo(device.getSN())
                            emit(info)
                        } finally {
                            _focusInfoRefreshStateFlow.value = false
                        }

                        // 自动刷新未开启，单次查询后终止循环。
                        if (interval == null) break
                        // 自动刷新已开启，延时后进入下一轮循环。
                        delay(interval)
                    }
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = FocusInfo()
            )

    /**
     * Flow：全局焦点信息刷新状态（内部）。
     */
    private val _focusInfoRefreshStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Flow：全局焦点信息刷新状态。
     */
    val focusInfoRefreshStateFlow: StateFlow<Boolean> = _focusInfoRefreshStateFlow


    /**
     * 刷新全局焦点信息。
     */
    fun refreshFocusInfo() {
        focusRefreshTrigger.update { it + 1 }
    }

    fun isFocusInfoAutoRefresh(): Boolean =
        focusInfoRefreshIntervalFlow.value != null

    fun setFocusInfoAutoRefresh(enable: Boolean) {
        _focusInfoRefreshIntervalFlow.value = if (enable) 2.seconds else null
    }

    fun getSelectedExecutor(): ADBExecutor {
        return logicManager.getSelectedExecutor()
    }
}
