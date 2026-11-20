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
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo

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

    /**
     * 用户上次主动选择的屏幕序号。
     */
    @Volatile
    private var preferredID: String? = null

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
                flow {
                    if (display == null) {
                        emit(null)
                    } else {
                        val info = logicManager.getSelectedExecutor().getDisplaySize(display.sn, display.id)
                        emit(info)
                    }
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
                    } else {
                        val info = logicManager.getSelectedExecutor().getDisplayDensity(display.sn, display.id)
                        emit(info)
                    }
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
        /* 设备列表变化时，优先恢复用户上次选择的设备，未找到则选中第一项。 */
        viewModelScope.launch {
            displaysFlow.collect { displays ->
                _selectedDisplayFlow.value =
                    displays.firstOrNull { it.id == preferredID } ?: displays.firstOrNull()
            }
        }

        viewModelScope.launch {
            selectedDisplayFlow.collect { display ->
                // 选中屏幕变更时，刷新一次尺寸与密度信息。
                if (display != null) {
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
        preferredID = display.id
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
                val result = executor.setDisplaySize(display.sn, width, height, display.id)
                if (result) {
                    onResult(null)
                } else {
                    onResult(Exception("修改分辨率失败"))
                }

                refreshDisplays()
                refreshSizeInfo()
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
                val result = executor.resetDisplaySize(display.sn, display.id)
                if (result) {
                    onResult(null)
                } else {
                    onResult(Exception("修改分辨率失败"))
                }

                refreshDisplays()
                refreshSizeInfo()
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
    fun setDisplayDensity(density: Int, onResult: (Exception?) -> Unit) {
        val executor = logicManager.getSelectedExecutor()
        val display = getSelectedDisplay() ?: return
        viewModelScope.launch {
            try {
                val result = executor.setDisplayDensity(display.sn, density, display.id)
                if (result) {
                    onResult(null)
                } else {
                    onResult(Exception("修改分辨率失败"))
                }

                refreshDensityInfo()
            } catch (e: Exception) {
                onResult(e)
            }
        }
    }

    /**
     * 还原当前屏幕的密度。
     */
    fun resetDisplayDensity(onResult: (Exception?) -> Unit) {
        val executor = logicManager.getSelectedExecutor()
        val display = getSelectedDisplay() ?: return
        viewModelScope.launch {
            try {
                val result = executor.resetDisplayDensity(display.sn, display.id)
                if (result) {
                    onResult(null)
                } else {
                    onResult(Exception("修改分辨率失败"))
                }

                refreshDensityInfo()
            } catch (e: Exception) {
                onResult(e)
            }
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
