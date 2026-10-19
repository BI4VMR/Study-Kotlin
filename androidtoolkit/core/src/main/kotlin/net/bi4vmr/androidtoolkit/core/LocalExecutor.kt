package net.bi4vmr.androidtoolkit.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver

/**
 * 本地执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object LocalExecutor : ADBExecutor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val devicesFlow: StateFlow<List<IDevice>> = ADBObserver.devicesState
        .map { list -> list.map { LocalDevice(it) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun getKey(): String = "local"

    override fun getType(): ExecutorType = ExecutorType.LOCAL

    override fun getDisplayName(): String = "本机"

    override fun getAvailableFlow(): StateFlow<Boolean> = ADBObserver.serviceState

    override fun getDevicesFlow(): StateFlow<List<IDevice>> = devicesFlow
}
