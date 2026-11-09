package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.java.common.base.id.IDUtil
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
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


    // 本地执行器固定返回无效 ID
    override fun getID(): String = IDUtil.UUID_NL_INVALID

    override fun getType(): ExecutorType = ExecutorType.LOCAL

    /**
     * 连接本地 ADB 服务，可重复调用。已就绪或正在准备时忽略。
     */
    override fun init() {
        adbObserver.init()
        taskScope.launch {
            ADBController.init()
        }
    }

    override fun release() {
        adbObserver.terminate()
        ADBController.terminate()
    }

    override fun getDisplayName(): String = "本机"

    override fun getStateFlow(): StateFlow<ServiceState> = adbObserver.serviceStateFlow

    override fun getExceptionFlow(): StateFlow<Exception?> = adbObserver.initErrorStateFlow

    override fun getDevicesFlow(): StateFlow<List<ADBDevice>> = adbObserver.devicesFlow

    override suspend fun getDisplays(sn: String): List<DisplayInfo> = withContext(Dispatchers.IO) {
        ADBCLIUtil.getDisplays(sn)
    }

    override suspend fun runForText(sn: String, command: String): String? = withContext(Dispatchers.IO) {
        ADBCLIUtil.runForText(sn, command)
    }

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

    override suspend fun getName(sn: String, fallback: String): String = withContext(Dispatchers.IO) {
        ADBController.getDevice(sn)?.getName(fallback) ?: fallback
    }

    override suspend fun getDeviceType(sn: String): DeviceType? = ADBController.getDevice(sn)?.getType()

    override fun openShell(sn: String) {
        ADBCLIUtil.openShell(sn)
    }
}
