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
import kotlinx.coroutines.withContext
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice

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

    // 刷新触发器：分辨率与密度
    private val metricsRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 当前选中屏幕的分辨率与密度。
     */
    val metricsFlow: StateFlow<DisplayMetrics> =
        combine(selectedDisplayFlow, metricsRefreshTrigger) { display, _ -> display }
            .flatMapLatest { display ->
                flow {
                    if (display == null) {
                        emit(DisplayMetrics())
                        return@flow
                    }

                    val executor = logicManager.selectedExecutorFlow.value
                    val metrics = runCatching {
                        DisplayMetrics(
                            size = executor.getDisplaySize(display.sn, display.id),
                            density = executor.getDisplayDensity(display.sn, display.id)
                        )
                    }.getOrElse { DisplayMetrics() }
                    emit(metrics)
                }.flowOn(Dispatchers.IO)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DisplayMetrics()
            )

    // 刷新触发器：界面组件列表
    private val componentsRefreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 当前选中的界面组件选项卡。
     */
    private val _componentTabFlow: MutableStateFlow<ComponentTab> = MutableStateFlow(ComponentTab.ACTIVITIES)
    val componentTabFlow: StateFlow<ComponentTab> = _componentTabFlow

    /**
     * Flow : 是否列出所有屏幕的界面组件。
     *
     * 为 `false` 时仅列出当前选中屏幕的界面组件。
     */
    private val _allDisplaysFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val allDisplaysFlow: StateFlow<Boolean> = _allDisplaysFlow

    /**
     * Flow : 界面组件列表。
     *
     * 跟随当前选中的设备、屏幕、选项卡与筛选条件。
     */
    val componentsFlow: StateFlow<ComponentsState> =
        combine(
            logicManager.selectedDeviceFlow,
            selectedDisplayFlow,
            componentTabFlow,
            allDisplaysFlow,
            componentsRefreshTrigger
        ) { device, display, tab, all, _ ->
            ComponentsQuery(device?.getSN(), if (all) null else display?.id, all, tab)
        }.flatMapLatest { query ->
            flow {
                val sn = query.sn
                if (sn == null || (!query.all && query.displayID == null)) {
                    emit(ComponentsState(tab = query.tab))
                    return@flow
                }

                emit(ComponentsState(tab = query.tab, loading = true))
                val executor = logicManager.selectedExecutorFlow.value
                val state = runCatching {
                    when (query.tab) {
                        ComponentTab.ACTIVITIES -> ComponentsState(
                            tab = query.tab,
                            activities = executor.getActivities(sn, query.displayID)
                        )

                        ComponentTab.FOREGROUND_ACTIVITIES -> ComponentsState(
                            tab = query.tab,
                            activities = executor.getForegroundActivities(sn, query.displayID)
                        )

                        ComponentTab.WINDOWS -> ComponentsState(
                            tab = query.tab,
                            windows = executor.getWindows(sn, query.displayID)
                        )

                        ComponentTab.FOREGROUND_WINDOWS -> ComponentsState(
                            tab = query.tab,
                            windows = executor.getForegroundWindows(sn, query.displayID)
                        )
                    }
                }.getOrElse { ComponentsState(tab = query.tab) }
                emit(state)
            }.flowOn(Dispatchers.IO)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ComponentsState()
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

    /**
     * 修改当前屏幕的分辨率。
     *
     * @param[onResult] 操作结果回调，在主线程调用。
     */
    fun setDisplaySize(width: Int, height: Int, onResult: (Boolean) -> Unit) =
        modifyMetrics(onResult) { sn, id -> setDisplaySize(sn, width, height, id) }

    /**
     * 还原当前屏幕的分辨率。
     */
    fun resetDisplaySize(onResult: (Boolean) -> Unit) =
        modifyMetrics(onResult) { sn, id -> resetDisplaySize(sn, id) }

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
            metricsRefreshTrigger.update { it + 1 }
            refreshDisplays()
            onResult(result)
        }
    }

    /**
     * 选择界面组件选项卡。
     */
    fun selectComponentTab(tab: ComponentTab) {
        _componentTabFlow.value = tab
    }

    /**
     * 设置是否列出所有屏幕的界面组件。
     */
    fun setAllDisplays(all: Boolean) {
        _allDisplaysFlow.value = all
    }

    /**
     * 刷新界面组件列表。
     */
    fun refreshComponents() {
        componentsRefreshTrigger.update { it + 1 }
    }


    /**
     * 界面组件查询条件。
     */
    private data class ComponentsQuery(
        val sn: String?,
        val displayID: String?,
        val all: Boolean,
        val tab: ComponentTab
    )
}


/**
 * 界面组件选项卡。
 */
enum class ComponentTab(val label: String) {
    ACTIVITIES("Activity 列表"),
    FOREGROUND_ACTIVITIES("前台 Activity"),
    WINDOWS("窗口列表"),
    FOREGROUND_WINDOWS("前台窗口")
}

/**
 * 界面组件列表状态。
 */
data class ComponentsState(
    val tab: ComponentTab = ComponentTab.ACTIVITIES,
    val loading: Boolean = false,
    val activities: List<ActivityInfo> = emptyList(),
    val windows: List<WindowInfo> = emptyList()
)

/**
 * 屏幕分辨率与密度。
 */
data class DisplayMetrics(
    val size: DisplaySize? = null,
    val density: DisplayDensity? = null
)
