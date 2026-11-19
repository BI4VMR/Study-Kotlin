@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.ui.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.common.model.*

/**
 * ViewModel : 显示页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DisplayPageViewModel(
    val globalStateManager: GlobalStateManager
) : ViewModel() {

    private val logicManager: LogicManager = globalStateManager.logicManager


    // 刷新触发器：屏幕列表
    private val displaysRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 屏幕列表。
     *
     * 跟随当前选中的设备，若无选中项则为空列表。
     */
    val displaysFlow: StateFlow<List<DisplayInfo>> =
        logicManager.selectedDeviceFlow
            .combine(displaysRefreshTrigger) { device, _ -> device }
            .flatMapLatest { device ->
                flow {
                    if (device == null) {
                        emit(emptyList())
                        return@flow
                    }

                    val displays = logicManager.getSelectedExecutor().getDisplays(device.getSN())
                    emit(displays.sortedBy { it.id })
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Flow : 当前选中的屏幕（内部）。
     */
    private val _selectedDisplayFlow: MutableStateFlow<DisplayInfo?> = MutableStateFlow(null)

    /**
     * Flow : 当前选中的屏幕。
     */
    val selectedDisplayFlow: StateFlow<DisplayInfo?> = _selectedDisplayFlow

    // 刷新触发器：屏幕尺寸信息
    private val sizeRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 屏幕尺寸信息。
     *
     * 跟随当前选中的屏幕，若无选中项则为空值。
     */
    val sizeInfoFlow: StateFlow<DisplaySize?> =
        selectedDisplayFlow
            .combine(sizeRefreshTrigger) { display, _ -> display }
            .flatMapLatest { display ->
                println("sizeInfoFlow change ${sizeRefreshTrigger.value} -> $display")
                flow {
                    if (display == null) {
                        emit(null)
                        return@flow
                    }

                    val apiLevel = logicManager.getSelectedExecutor().getAPILevel(display.sn).toFloatOrNull() ?: 0F
                    println("apiLevel -> $apiLevel")
                    if (apiLevel < 29F) {
                        println("display id -> ${display.id}")
                        if (display.id == "0") {
                            val info = logicManager.getSelectedExecutor().getDisplaySize(display.sn, null)
                            emit(info)
                            return@flow
                        } else {
                            println("sizeInfoFlow apiLevel < 30, emit null")
                            emit(null)
                            return@flow
                        }
                    }

                    val info = logicManager.getSelectedExecutor().getDisplaySize(display.sn, display.id)
                    println("sizeInfoFlow info ${info}")
                    emit(info)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    // 刷新触发器：像素密度信息
    private val densityRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 像素密度信息。
     *
     * 跟随当前选中的屏幕，若无选中项则为空值。
     */
    val densityInfoFlow: StateFlow<DisplayDensity?> =
        selectedDisplayFlow
            .combine(densityRefreshTrigger) { display, _ -> display }
            .flatMapLatest { display ->
                flow {
                    if (display == null) {
                        emit(null)
                        return@flow
                    }

                    val info = logicManager.getSelectedExecutor().getDisplayDensity(display.sn, display.id)
                    emit(info)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    // 刷新触发器：Activity 列表
    private val activitiesRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : Activity 列表。
     *
     * 跟随当前选中的屏幕，若无选中项则为空列表。
     */
    val activitiesFlow: StateFlow<List<ActivityInfo>> =
        selectedDisplayFlow
            .combine(activitiesRefreshTrigger) { display, _ -> display }
            .flatMapLatest { display ->
                flow {
                    if (display == null) {
                        emit(emptyList())
                        return@flow
                    }

                    val activities = logicManager.getSelectedExecutor().getActivities(display.sn, display.id)
                    emit(activities)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // 刷新触发器：Window 列表
    private val windowsRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : Window 列表。
     *
     * 跟随当前选中的屏幕，若无选中项则为空列表。
     */
    val windowsFlow: StateFlow<List<WindowInfo>> =
        selectedDisplayFlow
            .combine(windowsRefreshTrigger) { display, _ -> display }
            .flatMapLatest { display ->
                flow {
                    if (display == null) {
                        emit(emptyList())
                        return@flow
                    }

                    val activities = logicManager.getSelectedExecutor().getWindows(display.sn, display.id)
                    emit(activities)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )


    init {
        viewModelScope.launch {
            displaysFlow.collect { displays ->
                // 屏幕列表变更时，更新当前选中的屏幕。
                if (displays.isNotEmpty()) {
                    val selectedDisplay = selectedDisplayFlow.value
                    if (selectedDisplay == null || !displays.contains(selectedDisplay)) {
                        _selectedDisplayFlow.value = displays.first()
                    }
                } else {
                    _selectedDisplayFlow.value = null
                }
            }
        }

        viewModelScope.launch {
            selectedDisplayFlow.collect { display ->
                // 选中屏幕变更时，刷新一次尺寸与密度信息。
                if (display != null) {
                    println("selectedDisplayFlow change $display")
                    refreshSizeInfo()
                    refreshDensityInfo()
                }
            }
        }
    }


    /**
     * 选择屏幕。
     */
    fun selectDisplay(display: DisplayInfo) {
        _selectedDisplayFlow.value = display
    }

    fun getSelectedDisplay(): DisplayInfo? {
        return selectedDisplayFlow.value
    }

    /**
     * 刷新屏幕列表。
     */
    fun refreshDisplays() {
        displaysRefreshTrigger.update { it + 1 }
    }

    /**
     * 刷新尺寸信息。
     */
    fun refreshSizeInfo() {
        sizeRefreshTrigger.update { it + 1 }
    }

    /**
     * 修改当前屏幕的分辨率。
     *
     * @param[onResult] 操作结果回调，在主线程调用。
     */
    fun setDisplaySize(width: Int, height: Int, onResult: (Exception?) -> Unit) {
        val executor = logicManager.getSelectedExecutor()
        val display = getSelectedDisplay() ?: return
        viewModelScope.launch {
            try {
                val apiLevel = logicManager.getSelectedExecutor().getAPILevel(display.sn).toFloatOrNull() ?: 0F
                if (apiLevel < 29F) {
                    if (display.id == "0") {
                        executor.setDisplaySize(display.sn, width, height, null)
                    } else {
                        onResult(Exception("API Level < 29, only default display can be modified."))
                        return@launch
                    }
                }else{
                    executor.setDisplaySize(display.sn, width, height, display.id)
                }

                refreshDisplays()
                refreshSizeInfo()
                onResult(null)
            } catch (e: Exception) {
                onResult(e)
            }
        }
    }

    /**
     * 还原当前屏幕的分辨率。
     */
    fun resetDisplaySize(onResult: (Exception?) -> Unit) {
        val executor = logicManager.getSelectedExecutor()
        val display = getSelectedDisplay() ?: return
        viewModelScope.launch {
            try {
                val apiLevel = logicManager.getSelectedExecutor().getAPILevel(display.sn).toFloatOrNull() ?: 0F
                if (apiLevel < 29F) {
                    if (display.id == "0") {
                        executor.resetDisplaySize(display.sn,  null)
                    } else {
                        onResult(Exception("API Level < 29, only default display can be modified."))
                        return@launch
                    }
                }else{
                    executor.resetDisplaySize(display.sn, display.id)
                }

                refreshDisplays()
                refreshSizeInfo()
                onResult(null)
            } catch (e: Exception) {
                onResult(e)
            }
        }
    }

    /**
     * 刷新像素密度信息。
     */
    fun refreshDensityInfo() {
        densityRefreshTrigger.update { it + 1 }
    }

    /**
     * 修改当前屏幕的密度。
     */
    fun setDisplayDensity(density: Int, onResult: (Boolean) -> Unit) =
        modifyMetrics(onResult) { sn, id -> setDisplayDensity(sn, density, id) }

    /**
     * 还原当前屏幕的密度。
     */
    fun resetDisplayDensity(onResult: (Boolean) -> Unit) =
        modifyMetrics(onResult) { sn, id -> resetDisplayDensity(sn, id) }

    /**
     * 对当前屏幕执行修改操作，完成后刷新屏幕信息。
     */
    private fun modifyMetrics(
        onResult: (Boolean) -> Unit,
        action: suspend ADBExecutor.(sn: String, displayID: String) -> Boolean
    ) {
        val display = selectedDisplayFlow.value ?: return
        val executor = logicManager.selectedExecutorFlow.value
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { executor.action(display.sn, display.id) }.getOrDefault(false)
            }
            // metricsRefreshTrigger.update { it + 1 }
            refreshDisplays()
            onResult(result)
        }
    }

    /**
     * 开始投屏。
     */
    fun startCast() {
        getSelectedDisplay()?.let { display ->
            val device = logicManager.getSelectedDevice()
            if (device != null) {
                val record = CastRecord(logicManager.getSelectedExecutor(), device, display.id)
                globalStateManager.windowManager.addWindow(record)
            }
        }
    }
}
