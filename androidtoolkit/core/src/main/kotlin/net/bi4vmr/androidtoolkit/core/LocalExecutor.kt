package net.bi4vmr.androidtoolkit.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver

/**
 * 本地执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object LocalExecutor : ADBExecutor {

    private val adbObserver: ADBObserver = ADBObserver()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val devicesFlow: StateFlow<List<Device>> = adbObserver.devicesState
        .map { list -> list.map { LocalDevice(it) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun getType(): ExecutorType = ExecutorType.LOCAL

    override fun init() {
        adbObserver.init()
    }

    override fun release() {
        adbObserver.terminate()
    }

    override fun getDisplayName(): String = "本机"

    override fun getStateFlow(): StateFlow<Boolean> {
        // TODO 本地执行器总是可用的，直接返回 true。
        return MutableStateFlow(true)
    }

    override fun getDevicesFlow(): StateFlow<List<Device>> = devicesFlow
}
