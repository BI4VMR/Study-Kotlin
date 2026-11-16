@file:OptIn(ExperimentalCoroutinesApi::class)

package net.bi4vmr.androidtoolkit.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.DeviceFile

/**
 * ViewModel : 文件管理页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class FilesPageViewModel(
    private val logicManager: LogicManager
) : ViewModel() {

    /**
     * 目录加载状态。
     */
    sealed interface ListState {

        /** 未选择设备。 */
        data object NoDevice : ListState

        /** 正在加载。 */
        data object Loading : ListState

        /** 加载失败（路径无效或无权限）。 */
        data class Error(val path: String) : ListState

        /** 加载成功。 */
        data class Success(val files: List<DeviceFile>) : ListState
    }

    private val backStack: ArrayDeque<String> = ArrayDeque()
    private val forwardStack: ArrayDeque<String> = ArrayDeque()

    private val _currentPathFlow: MutableStateFlow<String> = MutableStateFlow(ROOT)

    /**
     * Flow : 当前目录路径。
     */
    val currentPathFlow: StateFlow<String> = _currentPathFlow

    private val _canGoBackFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canGoBackFlow: StateFlow<Boolean> = _canGoBackFlow

    private val _canGoForwardFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canGoForwardFlow: StateFlow<Boolean> = _canGoForwardFlow

    // 刷新触发器
    private val refreshTrigger = MutableStateFlow(0)

    /**
     * Flow : 当前目录的文件列表。
     */
    val listStateFlow: StateFlow<ListState> =
        combine(logicManager.selectedDeviceFlow, _currentPathFlow, refreshTrigger) { device, path, _ ->
            device to path
        }.flatMapLatest { (device, path) ->
            flow {
                if (device == null) {
                    emit(ListState.NoDevice)
                    return@flow
                }
                emit(ListState.Loading)
                val files = logicManager.selectedExecutorFlow.value.listFiles(device.getSN(), path)
                emit(if (files == null) ListState.Error(path) else ListState.Success(files))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListState.NoDevice
        )

    init {
        // 切换设备后回到根目录并清空历史
        logicManager.selectedDeviceFlow
            .map { it?.getSN() }
            .distinctUntilChanged()
            .onEach {
                backStack.clear()
                forwardStack.clear()
                _currentPathFlow.value = ROOT
                updateHistoryState()
            }
            .launchIn(viewModelScope)
    }


    /**
     * 跳转到指定路径。
     *
     * @param[path] 目标路径，支持 `.` 与 `..` 。相对路径基于当前目录解析。
     */
    fun navigateTo(path: String) {
        val target = normalize(path, _currentPathFlow.value)
        if (target == _currentPathFlow.value) {
            refresh()
            return
        }
        backStack.addLast(_currentPathFlow.value)
        forwardStack.clear()
        _currentPathFlow.value = target
        updateHistoryState()
    }

    /**
     * 进入上级目录。
     */
    fun goUp() {
        if (_currentPathFlow.value != ROOT) {
            navigateTo("..")
        }
    }

    fun goBack() {
        val prev = backStack.removeLastOrNull() ?: return
        forwardStack.addLast(_currentPathFlow.value)
        _currentPathFlow.value = prev
        updateHistoryState()
    }

    fun goForward() {
        val next = forwardStack.removeLastOrNull() ?: return
        backStack.addLast(_currentPathFlow.value)
        _currentPathFlow.value = next
        updateHistoryState()
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    private fun updateHistoryState() {
        _canGoBackFlow.value = backStack.isNotEmpty()
        _canGoForwardFlow.value = forwardStack.isNotEmpty()
    }


    companion object {

        const val ROOT = "/"

        /**
         * 规范化路径：解析 `.` 、 `..` 与重复的分隔符。
         */
        fun normalize(path: String, base: String): String {
            val raw = path.trim()
            val full = if (raw.startsWith("/")) raw else "$base/$raw"
            val segments = ArrayDeque<String>()
            full.split("/").forEach { seg ->
                when (seg) {
                    "", "." -> {}
                    ".." -> segments.removeLastOrNull()
                    else -> segments.addLast(seg)
                }
            }
            return "/" + segments.joinToString("/")
        }
    }
}
