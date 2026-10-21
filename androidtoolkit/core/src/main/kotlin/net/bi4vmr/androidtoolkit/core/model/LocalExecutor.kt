package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver

/**
 * 本地执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object LocalExecutor : ADBExecutor {

    private val adbObserver: ADBObserver = ADBObserver()

    private val taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val devicesFlow: StateFlow<List<ADBDevice>> = adbObserver.devicesFlow
        .stateIn(taskScope, SharingStarted.Eagerly, emptyList())


    init {
        // 单例对象，被访问时自动初始化。
        init()
    }


    override fun getType(): ExecutorType = ExecutorType.LOCAL

    override fun init() {
        adbObserver.init()
    }

    override fun release() {
        adbObserver.terminate()
    }

    override fun getDisplayName(): String = "本机"

    override fun getStateFlow(): StateFlow<ServiceState> = adbObserver.serviceStateFlow

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = devicesFlow
}
