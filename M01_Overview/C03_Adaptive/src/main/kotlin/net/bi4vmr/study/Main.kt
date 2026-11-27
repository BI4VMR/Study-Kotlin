package net.bi4vmr.study

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Material3 自适应布局示例入口。
 *
 * 运行后可以拖动窗口边缘改变宽度，观察列表-详情布局在单栏与双栏之间自动切换。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Material3 自适应布局示例",
        // 初始窗口宽度大于 840dp，启动时即可看到双栏布局。
        state = rememberWindowState(width = 1000.dp, height = 700.dp),
    ) {
        MaterialTheme {
            MailApp()
        }
    }
}
