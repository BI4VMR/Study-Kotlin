package net.bi4vmr.androidtoolkit.core.model

import com.android.ddmlib.FileListingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.java.common.base.id.IDUtil
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver
import java.io.File

/**
 * 本地执行器。
 *
 * 使用本地 ADB 进程操作本机连接的设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object LocalExecutor : ADBExecutor {

    private val adbObserver: ADBObserver = ADBObserver()

    private var taskScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun init() {
        adbObserver.init()
        taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        taskScope.launch {
            ADBController.init()
        }
    }

    override fun release() {
        taskScope.cancel()

        adbObserver.terminate()
        ADBController.terminate()
    }

    // 本地执行器固定返回无效 ID
    override fun getID(): String = IDUtil.UUID_NL_INVALID

    override fun getType(): ExecutorType = ExecutorType.LOCAL

    override suspend fun getDisplayName(): String = "本地"

    override fun getStateFlow(): StateFlow<ServiceState> = adbObserver.serviceStateFlow

    override fun getExceptionFlow(): StateFlow<Exception?> = adbObserver.serviceErrorStateFlow

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = adbObserver.devicesFlow


    /*
     * ----- 常用命令 -----
     */

    override suspend fun runForText(sn: String, command: String): String? = withContext(Dispatchers.IO) {
        ADBCLIUtil.runForText(sn, command)
    }

    override fun openShell(sn: String) {
        ADBCLIUtil.openShell(sn)
    }

    override suspend fun root(sn: String): Int =
        ADBCLIUtil.root(sn)

    override suspend fun reboot(sn: String, option: String): Int =
        ADBCLIUtil.reboot(sn, option)


    /*
     * ----- 设备信息 -----
     */

    override suspend fun getManufacturer(sn: String): String = withContext(Dispatchers.IO) {
        ADBCLIUtil.getManufacturer(sn)
    }

    override suspend fun getBrand(sn: String): String = withContext(Dispatchers.IO) {
        ADBCLIUtil.getBrand(sn)
    }

    override suspend fun getModel(sn: String): String = withContext(Dispatchers.IO) {
        ADBCLIUtil.getModel(sn)
    }

    override suspend fun getOSVersion(sn: String): String = withContext(Dispatchers.IO) {
        ADBCLIUtil.getOSVersion(sn)
    }

    override suspend fun getAPILevel(sn: String): String = withContext(Dispatchers.IO) {
        ADBCLIUtil.getAPILevel(sn)
    }

    override suspend fun getDeviceName(sn: String, fallback: String): String = withContext(Dispatchers.IO) {
        ADBController.getDevice(sn)?.getName(fallback) ?: fallback
    }

    override suspend fun getDeviceType(sn: String): ConnectionType? = withContext(Dispatchers.IO) {
        ADBController.getDevice(sn)?.getType()
    }


    /*
     * ----- 图形界面 -----
     */

    override suspend fun getDisplays(sn: String): List<DisplayInfo> = withContext(Dispatchers.IO) {
        ADBCLIUtil.getDisplays(sn)
    }

    override suspend fun getDisplaySize(sn: String, displayID: String): DisplaySize? =
        withContext(Dispatchers.IO) { ADBCLIUtil.getDisplaySize(sn, displayID) }

    override suspend fun setDisplaySize(sn: String, width: Int, height: Int, displayID: String): Boolean =
        withContext(Dispatchers.IO) { ADBCLIUtil.setDisplaySize(sn, width, height, displayID) }

    override suspend fun resetDisplaySize(sn: String, displayID: String): Boolean =
        withContext(Dispatchers.IO) { ADBCLIUtil.resetDisplaySize(sn, displayID) }

    override suspend fun getDisplayDensity(sn: String, displayID: String): DisplayDensity? =
        withContext(Dispatchers.IO) { ADBCLIUtil.getDisplayDensity(sn, displayID) }

    override suspend fun setDisplayDensity(sn: String, density: Int, displayID: String): Boolean =
        withContext(Dispatchers.IO) { ADBCLIUtil.setDisplayDensity(sn, density, displayID) }

    override suspend fun resetDisplayDensity(sn: String, displayID: String): Boolean =
        withContext(Dispatchers.IO) { ADBCLIUtil.resetDisplayDensity(sn, displayID) }

    override suspend fun getActivities(sn: String, displayID: String?): List<ActivityInfo> =
        withContext(Dispatchers.IO) { ADBCLIUtil.getActivities(sn, displayID) }

    override suspend fun getWindows(sn: String, displayID: String?): List<WindowInfo> =
        withContext(Dispatchers.IO) { ADBCLIUtil.getWindows(sn, displayID) }

    override suspend fun getGlobalFocusInfo(sn: String): FocusInfo =
        withContext(Dispatchers.IO) { ADBCLIUtil.getGlobalFocusInfo(sn) }

    override suspend fun screenshot(sn: String, file: File, displayID: String) =
        withContext(Dispatchers.IO) { ADBCLIUtil.screenshot(sn, file, displayID) }

    override suspend fun screenrecord(sn: String, file: File, displayID: String) =
        withContext(Dispatchers.IO) { ADBCLIUtil.screenrecord(sn, file, displayID) }


    /*
     * ----- 文件管理 -----
     */

    override suspend fun listFiles(sn: String, path: String): List<DeviceFile>? = withContext(Dispatchers.IO) {
        val device = ADBController.getDevice(sn) ?: return@withContext null
        runCatching {
            // 从根节点逐级向下查找目标目录
            var current: FileListingService.FileEntry = device.getFilesRoot()
            path.split("/")
                .filter { it.isNotEmpty() }
                .forEach { segment ->
                    val child = device.listFiles(current).find { it.name == segment }
                    if (child == null || !child.isDirectory) {
                        return@withContext null
                    }
                    current = child
                }

            device.listFiles(current)
                .map { it.toDeviceFile() }
                .sortedWith(compareByDescending<DeviceFile> { it.isDirectory }.thenBy { it.name.lowercase() })
        }.getOrNull()
    }

    private fun FileListingService.FileEntry.toDeviceFile(): DeviceFile {
        val isLink = (type == FileListingService.TYPE_LINK) || (type == FileListingService.TYPE_DIRECTORY_LINK)
        return DeviceFile(
            name = name,
            path = fullPath,
            isDirectory = isDirectory,
            isLink = isLink,
            size = sizeValue.toLong(),
            modifyTime = listOfNotNull(date, time).joinToString(" "),
            permissions = permissions ?: "",
            linkTarget = if (isLink) info?.removePrefix("-> ")?.trim() else null
        )
    }

    override suspend fun pushFile(
        sn: String,
        local: String,
        remote: String,
        refreshMediaDB: Boolean,
        listener: ADBFileTransferListener?
    ) {
        val device = ADBController.requireDevice(sn)
        device.pushFile(local, remote, refreshMediaDB, listener)
    }


    /*
     * ----- 软件包管理 -----
     */

    override suspend fun listPackages(sn: String, type: PackageType): List<String> = withContext(Dispatchers.IO) {
        ADBCLIUtil.listPackages(sn, type)
    }
}
