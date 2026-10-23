package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.cast.compose.ADBLocalCastView
import net.bi4vmr.tool.kotlin.external.adb.cast.compose.ADBRemoteCastView
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.androidtoolkit.ui.GlobalWindowManager

/**
 * 设备投屏窗口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Composable
fun ADBCastWindow(
    globalStateManager: GlobalStateManager,
    record: CastRecord
) {
    Window(
        onCloseRequest = { globalStateManager.windowManager.removeWindow(record) },
        title = "投屏 - ${record.device.getSN()} (Display ${record.displayID})"
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val device = record.device
            val executor = record.executor
            when {
                device is LocalDevice -> ADBLocalCastView(
                    device = device,
                    displayID = record.displayID,
                    modifier = Modifier.fillMaxSize()
                )

                executor is RemoteExecutor -> ADBRemoteCastView(
                    client = executor.client,
                    deviceSN = device.getSN(),
                    displayID = record.displayID,
                    modifier = Modifier.fillMaxSize()
                )

                else -> Text("不支持的设备类型")
            }
        }
    }
}

/**
 * 投屏窗口记录。
 */
class CastRecord(
    val executor: ADBExecutor,
    val device: ADBDevice,
    val displayID: String
) : GlobalWindowManager.Record()
