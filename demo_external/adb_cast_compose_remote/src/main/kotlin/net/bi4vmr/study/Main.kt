package net.bi4vmr.study

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.bi4vmr.tool.kotlin.external.adb.cast.compose.ADBRemoteCastView

/**
 * 示例： ADB 屏幕投射控件。
 *
 * 调试方法：
 *
 * `./gradlew :adb_cast_compose_demo:run`
 */
fun main() = application {
    val windowState = rememberWindowState()

    Window(
        title = "示例：ADB 屏幕投射控件",
        state = windowState,
        onCloseRequest = ::exitApplication
    ) {
        var host: String by remember { mutableStateOf("127.0.0.1") }
        var port: String by remember { mutableStateOf("30000") }
        var state: Boolean by remember { mutableStateOf(false) }

        Column {
            Row(
                horizontalArrangement = spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("主机：") }
                )
                TextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口：") }
                )
                Button(onClick = {
                    state = !state
                }) {
                    Text("连接")
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state) {
                    ADBRemoteCastView(
                        host = host,
                        port = port.toInt(),
                        onVideoSizeChange = { w, h ->
                            println("OnVideoSizeChange. W:[$w] H:[$h]")
                            // 视频尺寸变更时，同步更新窗口尺寸。
                            windowState.size = DpSize(width = w.dp, height = h.dp)
                        })
                } else {
                    Text("未连接")
                }
            }
        }
    }
}
