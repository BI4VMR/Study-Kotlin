package net.bi4vmr.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastManager
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBLocalCastView

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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 设置 Scrcpy 服务端文件路径
            ADBCastManager.setServerFilePath("C:\\Users\\bi4vmr\\Download\\scrcpy-server-v4.1")
            // ADBCastManager.setServerFilePath("/home/bi4vmr/Downloads/scrcpy-server-v5.0.1")

            // 连接本地 ADB 进程
            if (!ADBController.isInitialized()) {
                ADBController.init()
            }

            // 打开首个设备的投屏视图
            ADBController.getDevices()
                .firstOrNull()
                ?.let { device ->
                    ADBLocalCastView(
                        device = device,
                        onVideoSizeChange = { w, h ->
                            println("OnVideoSizeChange. W:[$w] H:[$h]")
                            // 视频尺寸变更时，同步更新窗口尺寸。
                            windowState.size = DpSize(width = w.dp, height = h.dp)
                        })
                }
                ?: Text("无可用设备！")
        }
    }
}
