package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.GlobalWindowManager
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.common_ic_cast
import net.bi4vmr.androidtoolkit.ui.util.getAvailableSize
import net.bi4vmr.tool.kotlin.external.adb.cast.compose.ADBLocalCastView
import net.bi4vmr.tool.kotlin.external.adb.cast.compose.ADBRemoteCastView
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import org.jetbrains.compose.resources.painterResource

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
    val windowState = rememberWindowState(
        size = DpSize(800.dp, 600.dp)
    )


    Window(
        title = "${record.device.getSN()}  # ${record.displayID}",
        icon = painterResource(Res.drawable.common_ic_cast),
        state = windowState,
        onCloseRequest = { globalStateManager.windowManager.removeWindow(record) }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val device = record.device
            val executor = record.executor
            when {
                device is LocalDevice -> ADBLocalCastView(
                    device = device,
                    displayID = record.displayID,
                    modifier = Modifier.fillMaxSize(),
                    onVideoSizeChange = { width, height ->
                        println("Video size changed: $width x $height")
                        val maxWinAreaSize = window.getAvailableSize()
                        println("maxWinAreaSize: $maxWinAreaSize")
                        val newWidth = width.coerceAtMost(maxWinAreaSize.width.value.toInt())
                        val newHeight = height.coerceAtMost(maxWinAreaSize.height.value.toInt())
                        println("new size: $newWidth x $newHeight")
                        // windowState.size = DpSize(newWidth.dp, newHeight.dp)
                    }
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
