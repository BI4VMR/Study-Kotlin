package net.bi4vmr.androidtoolkit.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.bi4vmr.androidtoolkit.ui.App

/**
 * 应用程序入口。
 *
 * 调试命令：
 *
 * `./gradlew :androidtoolkit:app:run --console=plain`
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Android Toolkit"
    ) {
        App()
    }
}
