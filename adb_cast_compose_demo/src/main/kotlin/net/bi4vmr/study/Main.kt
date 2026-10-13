package net.bi4vmr.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastManager
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastView

/**
 * `./gradlew :adb_cast_compose_demo:run`
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose ADB"
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ADBCastManager.setServerFilePath("C:\\Users\\bi4vmr\\Download\\scrcpy-server-v4.1")
            ADBController.init()
            ADBController.getDevices().firstOrNull()?.let { device ->
                ADBCastView(device = device)
            } ?: Text("No Device!")
        }
    }
}
