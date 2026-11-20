package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.*
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient

/**
 * 远程执行器。
 *
 * 通过 ADB 代理服务端操作远程主机连接的设备。
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

    override fun getID(): String = id

    override fun getType(): ExecutorType = ExecutorType.REMOTE

    override suspend fun getDisplayName(): String = name

    override fun getStateFlow(): StateFlow<ServiceState> = client.serviceStateFlow

    override fun getExceptionFlow(): StateFlow<Exception?> = client.errorFlow

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = client.devicesFlow


    /*
     * ----- 常用命令 -----
     */

    override suspend fun runForText(sn: String, command: String): String? = client.runForText(sn, command)

    override fun openShell(sn: String) {
        client.openShell(sn)
    }

    override fun root(sn: String): Int {
        TODO("Not yet implemented")
    }

    override fun reboot(sn: String, option: String): Int {
        TODO("Not yet implemented")
    }


    /*
     * ----- 设备信息 -----
     */

    override suspend fun getManufacturer(sn: String): String = client.getManufacturer(sn)

    override suspend fun getBrand(sn: String): String = client.getBrand(sn)

    override suspend fun getModel(sn: String): String = client.getModel(sn)

    override suspend fun getOSVersion(sn: String): String = client.getOSVersion(sn)

    override suspend fun getAPILevel(sn: String): String = client.getAPILevel(sn)

    override suspend fun getDeviceName(sn: String, fallback: String): String = client.getName(sn, fallback)

    override suspend fun getDeviceType(sn: String): ConnectionType? = runCatching { client.getType(sn) }.getOrNull()


    /*
     * ----- 图形界面 -----
     */

    override suspend fun getDisplays(sn: String): List<DisplayInfo> =
        client.getDisplays(sn)

    override suspend fun getDisplaySize(sn: String, displayID: String): DisplaySize? =
        client.getDisplaySize(sn, displayID)

    override suspend fun setDisplaySize(sn: String, width: Int, height: Int, displayID: String): Boolean =
        client.setDisplaySize(sn, width, height, displayID)

    override suspend fun resetDisplaySize(sn: String, displayID: String): Boolean =
        client.resetDisplaySize(sn, displayID)

    override suspend fun getDisplayDensity(sn: String, displayID: String): DisplayDensity? =
        client.getDisplayDensity(sn, displayID)

    override suspend fun setDisplayDensity(sn: String, density: Int, displayID: String): Boolean =
        client.setDisplayDensity(sn, density, displayID)

    override suspend fun resetDisplayDensity(sn: String, displayID: String): Boolean =
        client.resetDisplayDensity(sn, displayID)

    override suspend fun getActivities(sn: String, displayID: String?): List<ActivityInfo> =
        client.getActivities(sn, displayID)

    override suspend fun getWindows(sn: String, displayID: String?): List<WindowInfo> =
        client.getWindows(sn, displayID)

    override suspend fun getForegroundWindows(sn: String, displayID: String?): List<WindowInfo> =
        client.getForegroundWindows(sn, displayID)

    override suspend fun getGlobalFocusInfo(sn: String): FocusInfo {
        TODO("Not yet implemented")
    }


    /*
     * ----- 文件管理 -----
     */

    override suspend fun listFiles(sn: String, path: String): List<DeviceFile>? = null


    /*
     * ----- 软件包管理 -----
     */

    override suspend fun listPackages(sn: String, type: PackageType): List<String> =
        client.listPackages(sn, type)
}
