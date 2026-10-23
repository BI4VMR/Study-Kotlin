package net.bi4vmr.tool.kotlin.external.adb.cast.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader

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
fun ADBCastView(
    stateManager: CastStateManager,
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
    // 当前状态
    val state: ADBCastState by stateManager.stateFlow.collectAsState()

    // 视频帧尺寸
    val frameSize: IntSize by stateManager.frameSizeFlow.collectAsState()

    // 画布尺寸
    var canvasSize: IntSize by remember { mutableStateOf(IntSize.Zero) }

    // 有效内容区域
    val renderRect: Rect = remember(canvasSize, frameSize) {
        fitCanvas(canvasSize, frameSize)
    }

    // 当前画面的 Shader
    val shader: Shader? by stateManager.shaderFlow.collectAsState()

    // 输入控制器
    val inputController: InputController? by stateManager.inputControllerFlow.collectAsState()

    // 错误消息
    val errorInfo: Exception? by stateManager.errorInfoFlow.collectAsState()


    // 视频帧尺寸变化时通知外部，以便其调整画面尺寸。
    LaunchedEffect(frameSize) {
        val width = frameSize.width
        val height = frameSize.height
        if (width > 0 && height > 0) {
            onVideoSizeChange?.invoke(width, height)
        }
    }


    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .onSizeChanged { canvasSize = it }
    ) {
        when (state) {
            /* 初始化 */
            // TODO 暂时未添加
            ADBCastState.IDLE -> LoadingView()
            /* 加载中 */
            ADBCastState.LOADING -> LoadingView()
            /* 正在投屏 */
            ADBCastState.CASTING -> {
                // 设备画面显示
                SkiaShaderLayer(
                    frameSize = frameSize,
                    shader = shader
                )

                if (allowInput) {
                    val scale = LocalDensity.current.density
                    val widthDP = (renderRect.width * scale).dp
                    val heightDP = (renderRect.height * scale).dp

                    // 触控叠加层
                    InputEffectLayer(
                        visible = inputVisible,
                        // 尺寸与有效区域保持一致
                        modifier = Modifier.size(widthDP, heightDP),
                        onPress = { position ->
                            inputLayerToFramePosition(position, renderRect, frameSize).let {
                                inputController?.mouseDown(it.x.toInt(), it.y.toInt())
                            }
                        },
                        onMove = { position ->
                            inputLayerToFramePosition(position, renderRect, frameSize).let {
                                inputController?.mouseMove(it.x.toInt(), it.y.toInt(), pressed = true)
                            }
                        },
                        onRelease = { position ->
                            inputLayerToFramePosition(position, renderRect, frameSize).let {
                                inputController?.mouseUp(it.x.toInt(), it.y.toInt())
                            }
                        },
                        onSecondaryClick = { _ ->
                            // 右键默认转发返回键（对齐scrcpy桌面客户端右键默认绑定BACK的行为）
                            inputController?.pressBack()
                        },
                        onScroll = { position, delta ->
                            inputLayerToFramePosition(position, renderRect, frameSize).let {
                                inputController?.scroll(
                                    position.x.toInt(),
                                    position.y.toInt(),
                                    // Compose的scrollDelta方向与scrcpy相反
                                    -delta
                                )
                            }
                        },
                        onClick = { position ->
                            inputLayerToFramePosition(position, renderRect, frameSize).let {
                                onClick?.invoke(it.x.toInt(), it.y.toInt())
                            }
                        },
                        onLongClick = { position ->
                            inputLayerToFramePosition(position, renderRect, frameSize).let {
                                onLongClick?.invoke(it.x.toInt(), it.y.toInt())
                            }
                        },
                        onSwipe = { start, end ->
                            val startFrame = inputLayerToFramePosition(start, renderRect, frameSize)
                            val endFrame = inputLayerToFramePosition(end, renderRect, frameSize)
                            onSwipe?.invoke(
                                startFrame.x.toInt(), startFrame.y.toInt(),
                                endFrame.x.toInt(), endFrame.y.toInt()
                            )
                        }
                    )
                }
            }
            // 出错终止
            ADBCastState.ERROR -> ErrorView(info = errorInfo?.message ?: "未知错误")
        }
    }
}
