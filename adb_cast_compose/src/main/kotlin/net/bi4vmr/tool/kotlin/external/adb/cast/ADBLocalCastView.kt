package net.bi4vmr.tool.kotlin.external.adb.cast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice

/**
 * Android设备屏幕投影视图。
 *
 * 利用Scrcpy服务端实现屏幕投屏功能，并支持转发触控事件以远程控制设备。
 *
 * @param[device] 要投屏的Android设备。
 * @param[displayID] 要投屏的显示信息，如果为null则使用设备的默认显示。
 * @param[modifier] Compose的Modifier。
 * @param[background] 设备画面未覆盖区域的颜色。
 * @param[allowInput] 是否转发触控事件。
 * @param[inputVisible] 是否显示触控事件动效。
 * @param[onVideoSizeChange] 视频尺寸变化回调，可以用于实现窗口跟随设备画面尺寸等功能。
 * @param[onClick] 点击手势回调，参数为设备帧坐标(x, y)。
 * @param[onLongClick] 长按手势回调，参数为设备帧坐标(x, y)。
 * @param[onSwipe] 拖拽手势回调，参数为设备帧坐标(startX, startY, endX, endY)。
 */
@Composable
fun ADBLocalCastView(
    device: ADBDevice,
    displayID: String = "0",
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF1E1F22),
    allowInput: Boolean = true,
    inputVisible: Boolean = true,
    onVideoSizeChange: ((width: Int, height: Int) -> Unit)? = null,
    onClick: ((x: Int, y: Int) -> Unit)? = null,
    onLongClick: ((x: Int, y: Int) -> Unit)? = null,
    onSwipe: ((startX: Int, startY: Int, endX: Int, endY: Int) -> Unit)? = null
) {
    val stateManager: ADBCastStateManager = remember(device, displayID) { ADBCastStateManager() }

    // 投屏任务控制
    DisposableEffect(device, displayID) {
        stateManager.startLocal(device, displayID)

        onDispose {
            stateManager.stop()
        }
    }

    ADBCastView(
        stateManager = stateManager,
        displayID = displayID,
        modifier = modifier,
        background = background,
        allowInput = allowInput,
        inputVisible = inputVisible,
        onVideoSizeChange = onVideoSizeChange,
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipe = onSwipe
    )
}
