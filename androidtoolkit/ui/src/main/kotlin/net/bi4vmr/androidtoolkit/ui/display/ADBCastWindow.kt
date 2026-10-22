package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.WindowManager

/**
 * 设备投屏窗口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Composable
fun ADBCastWindow(
    globalStateManager: GlobalStateManager,
    record: WindowManager.Record
) {
    Window(
        onCloseRequest = { globalStateManager.windowManager.removeWindow(record) },
        title = "投屏"
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        }
    }
}
