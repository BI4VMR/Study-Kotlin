package net.bi4vmr.androidtoolkit.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.composenativetray.tray.api.Tray
import net.bi4vmr.androidtoolkit.ui.App
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.app_name
import net.bi4vmr.androidtoolkit.ui.common_ic_app
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 应用程序入口。
 *
 * 调试命令：
 *
 * `./gradlew :androidtoolkit:app:run --console=plain`
 */
fun main() = application {
    val mainWindowState = rememberWindowState()
    var mainWindowVisible by remember { mutableStateOf(true) }

    Window(
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.common_ic_app),
        state = mainWindowState,
        visible = mainWindowVisible,
        onCloseRequest = ::exitApplication
    ) {
        App()
    }


    Tray(
        icon = painterResource(Res.drawable.common_ic_app),
        tooltip = stringResource(Res.string.app_name),
        primaryAction = { mainWindowVisible = !mainWindowVisible }
    )
}
