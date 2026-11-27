@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package net.bi4vmr.androidtoolkit.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.composenativetray.tray.api.Tray
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.App
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.app_name
import net.bi4vmr.androidtoolkit.ui.common_ic_app
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_WINDOW_WIDTH: Int = 680
private const val MIN_WINDOW_HEIGHT: Int = 520

/**
 * 应用程序入口。
 *
 * 调试命令：
 *
 * `./gradlew :androidtoolkit:app:run --console=plain`
 */
fun main() {
    // 启动时读取上次保存的窗口尺寸；同步读取以免窗口先以默认尺寸显示再跳变。
    val (savedWidth, savedHeight) = runBlocking {
        LogicManager.windowWidthFlow.first() to LogicManager.windowHeightFlow.first()
    }

    application {
        val mainWindowState = rememberWindowState(
            size = DpSize(
                savedWidth.coerceAtLeast(MIN_WINDOW_WIDTH).dp,
                savedHeight.coerceAtLeast(MIN_WINDOW_HEIGHT).dp
            )
        )
        var mainWindowVisible by remember { mutableStateOf(true) }

        Window(
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.common_ic_app),
            state = mainWindowState,
            visible = mainWindowVisible,
            onCloseRequest = {
                // 退出前同步保存一次，避免防抖期间退出时丢失最后的尺寸。
                runBlocking {
                    val size = mainWindowState.size
                    LogicManager.saveWindowSizeNow(
                        size.width.value.roundToInt(),
                        size.height.value.roundToInt()
                    )
                }
                exitApplication()
            }
        ) {
            // 限制拖拽时窗口最小尺寸
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)
            }

            // 监听窗口尺寸变化并持久化；防抖，避免拖拽过程中频繁写入配置文件。
            LaunchedEffect(mainWindowState) {
                snapshotFlow { mainWindowState.size }
                    // 500ms 内没有新的尺寸才发出最终值
                    .debounce(500.milliseconds)
                    .collect { size ->
                        LogicManager.setWindowSize(
                            size.width.value.roundToInt(),
                            size.height.value.roundToInt()
                        )
                    }
            }

            App()
        }


        Tray(
            icon = painterResource(Res.drawable.common_ic_app),
            tooltip = stringResource(Res.string.app_name),
            primaryAction = { mainWindowVisible = !mainWindowVisible }
        )
    }
}
