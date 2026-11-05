package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.ADBController
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

    override fun openShell(sn: String) {
        ADBCLIUtil.openShell(sn)
    }
}
