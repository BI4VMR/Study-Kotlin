package net.bi4vmr.androidtoolkit.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.bi4vmr.androidtoolkit.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Android Toolkit"
    ) {
        App()
    }
}
