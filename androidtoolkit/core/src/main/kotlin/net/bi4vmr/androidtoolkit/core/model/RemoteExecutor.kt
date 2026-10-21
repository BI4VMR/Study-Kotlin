package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient

/**
 * 远程执行器，通过 ADB 代理服务端操作其所在主机的设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RemoteExecutor(
    private val host: String,
    private val port: Int,
    private val client: ADBProxyClient
) : ADBExecutor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val devicesFlow: StateFlow<List<ADBDevice>> = client.devicesFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun getType(): ExecutorType = ExecutorType.REMOTE

    override fun getDisplayName(): String = "$host:$port"

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = devicesFlow

    override suspend fun getDisplays(sn: String): List<DisplayInfo> = client.getDisplays(sn)

    override fun getStateFlow(): StateFlow<ServiceState> {
        return client.serviceStateFlow
    }

    override fun release() {
        scope.cancel()
        client.release()
    }
}
