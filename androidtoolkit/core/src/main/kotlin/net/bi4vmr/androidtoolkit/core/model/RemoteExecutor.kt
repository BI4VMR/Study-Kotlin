package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient

/**
 * 远程执行器。
 *
 * 通过 ADB 代理服务端操作其所在主机的设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RemoteExecutor(

    /**
     * 数据库 ID 。
     */
    val id: String,

    /**
     * 目标地址。
     */
    val host: String,

    /**
     * 目标端口。
     */
    val port: Int
) : ADBExecutor {

    val client: ADBProxyClient = ADBProxyClient(host, port)

    @Volatile
    var name: String = "$host:$port"


    override fun init() {
        client.init()
    }

    override fun release() {
        client.stop()
    }

    override fun getType(): ExecutorType = ExecutorType.REMOTE

    override fun getID(): String = id

    override fun getDisplayName(): String = name

    override fun getStateFlow(): StateFlow<ServiceState> = client.serviceStateFlow

    override fun getExceptionFlow(): StateFlow<Exception?> = client.errorFlow

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = client.devicesFlow

    override suspend fun getDisplays(sn: String): List<DisplayInfo> = client.getDisplays(sn)

    override suspend fun runForText(sn: String, command: String): String? = client.runForText(sn, command)

    override suspend fun getManufacturer(sn: String): String = client.getManufacturer(sn)

    override suspend fun getBrand(sn: String): String = client.getBrand(sn)

    override suspend fun getModel(sn: String): String = client.getModel(sn)

    override suspend fun getOSVersion(sn: String): String = client.getOSVersion(sn)

    override suspend fun getAPILevel(sn: String): String = client.getAPILevel(sn)

    override suspend fun getName(sn: String, fallback: String): String = client.getName(sn, fallback)

    override suspend fun getDeviceType(sn: String): ConnectionType? = runCatching { client.getType(sn) }.getOrNull()

    override fun openShell(sn: String) {
        client.openShell(sn)
    }


    /*
     * ----- 软件包管理 -----
     */

    override suspend fun listPackages(sn: String, type: PackageType): List<String> =
        client.listPackages(sn, type)
}
