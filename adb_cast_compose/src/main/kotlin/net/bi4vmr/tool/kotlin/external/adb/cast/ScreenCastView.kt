package net.bi4vmr.tool.kotlin.external.adb.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bi4vmr.tool.java.external.adb.ADBDevice
import net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastEventListener
import net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastManager
import net.bi4vmr.tool.kotlin.external.scrcpy.control.InputController
import net.bi4vmr.tool.kotlin.external.scrcpy.video.VideoCodec
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
fun ScreenCastView(
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
    // 画布尺寸
    var canvasSize: IntSize by remember { mutableStateOf(IntSize.Zero) }

    // 视频帧尺寸
    var frameSize: IntSize by remember { mutableStateOf(IntSize.Zero) }

    // 有效内容区域
    val renderRect: Rect = remember(canvasSize, frameSize) {
        fitCanvas(canvasSize, frameSize)
    }

    // 当前画面的Shader
    var shader: Shader? by remember { mutableStateOf(null) }

    // 输入控制器
    var inputController: InputController? by remember { mutableStateOf(null) }

    // 错误消息
    var errorInfo: String by remember { mutableStateOf("") }

    // 状态
    var state: ScreenCastState by remember { mutableStateOf(ScreenCastState.Loading) }


    // 投屏事件监听器
    val listener: ScreenCastEventListener = remember {
        object : ScreenCastEventListener {

            override fun onSizeChange(width: Int, height: Int) {
                frameSize = IntSize(width, height)

                onVideoSizeChange?.invoke(width, height)
            }

            override fun onNewFrame(
                yData: ByteArray,
                yStride: Int,
                uData: ByteArray,
                uStride: Int,
                vData: ByteArray,
                vStride: Int,
                width: Int,
                height: Int
            ) {
                // 将YUV(I420)数据转换为Skia Shader
                shader = SkiaYUVRenderer.getShader(yData, yStride, uData, uStride, vData, vStride, width, height)
            }

            override fun onVideoReady() {
                state = ScreenCastState.Casting
            }

            override fun onControlReady(controller: InputController) {
                inputController = controller
            }

            override fun onError(error: Exception) {
                state = ScreenCastState.Error
                errorInfo = "${error.javaClass.simpleName}: ${error.message}"
            }
        }
    }


    DisposableEffect(device, displayID) {
        ScreenCastManager.start(device, listener, displayID, debug = true, videoCodec = VideoCodec.H264)

        onDispose {
            state = ScreenCastState.Loading

            ScreenCastManager.stop(listener)
            inputController = null
            shader = null
            frameSize = IntSize.Zero
            errorInfo = ""
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
            // 加载中
            ScreenCastState.Loading -> {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 8.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "等待设备视频信号...",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }
            }
            // 正在投影
            ScreenCastState.Casting -> {
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
            // 出现错误
            ScreenCastState.Error -> {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "非常不妙！出错了！",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorInfo,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}


/**
 * 屏幕投影状态。
 */
private enum class ScreenCastState {
    Loading,
    Casting,
    Error;
}
