package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.flow.StateFlow
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
    val host: String,
    val port: Int,
    val client: ADBProxyClient
) : ADBExecutor {

    override fun getType(): ExecutorType = ExecutorType.REMOTE

    override fun init() {
        client.init()
    }

    override fun release() {
        client.stop()
    }

    override fun getDisplayName(): String = "$host:$port"

    override fun getStateFlow(): StateFlow<ServiceState> = client.serviceStateFlow

    override fun getExceptionFlow(): StateFlow<Exception?> = client.errorFlow

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = client.devicesFlow

    override suspend fun getDisplays(sn: String): List<DisplayInfo> = client.getDisplays(sn)

    override suspend fun runForText(sn: String, command: String): String? = client.runForText(sn, command)

    override fun openShell(sn: String) {
        client.openShell(sn)
    }
}
