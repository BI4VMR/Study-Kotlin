package net.bi4vmr.androidtoolkit.ui.util

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.Toolkit

fun getWindowAvailableArea(window: ComposeWindow): Rect {
    // 屏幕尺寸
    val screenBounds = window.graphicsConfiguration.bounds
    // 系统 Insets（顶栏、底栏等）
    val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(window.graphicsConfiguration)
    // 当前窗口 Insets（标题栏）
    val windowInsets = window.insets

    val left = screenBounds.x + screenInsets.left + windowInsets.left
    val top = screenBounds.y + screenInsets.top + windowInsets.top
    val right =
        (screenBounds.x + screenBounds.width) - screenInsets.right - windowInsets.right
    val bottom =
        (screenBounds.y + screenBounds.height) - screenInsets.bottom - windowInsets.bottom

    return Rect(
        left.toFloat(),
        top.toFloat(),
        right.coerceAtLeast(0).toFloat(),
        bottom.coerceAtLeast(0).toFloat()
    )
}

fun ComposeWindow.getAvailableArea(): Rect {
    return getWindowAvailableArea(this)
}

fun getWindowAvailableSize(window: ComposeWindow): DpSize {
    val area = getWindowAvailableArea(window)
    return DpSize(area.width.toInt().dp, area.height.toInt().dp)
}

fun ComposeWindow.getAvailableSize(): DpSize {
    return getWindowAvailableSize(this)
}
