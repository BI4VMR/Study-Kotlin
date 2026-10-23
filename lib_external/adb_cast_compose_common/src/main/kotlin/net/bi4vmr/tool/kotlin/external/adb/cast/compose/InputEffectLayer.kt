@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package net.bi4vmr.tool.kotlin.external.adb.cast.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import org.jetbrains.skia.Rect

/**
 * 输入动效叠加层。
 *
 * 分为两层独立的指针处理：
 * 1. 原始事件层（[PointerEventPass.Initial]）：观察所有事件并立即转发，不消费事件；仅转发左键（或触摸设备）产生的按下/移动/抬起事件。
 * 2. 手势识别层：使用 [detectTapGestures] 和 [detectDragGestures] 识别高级手势，通过 [PointerMatcher] 参数分别限定响应左键（点击/长按/拖拽）与右键（点击）。
 *
 * 两层并行工作：实时转发不影响手势识别，手势识别不阻塞实时转发。
 *
 * @param visible 是否显示动效。
 * @param rippleColor 点击涟漪颜色。
 * @param rippleDuration 涟漪动画持续时间（毫秒）。
 * @param trailColor 滑动轨迹颜色。
 * @param trailWidth 滑动轨迹线宽。
 * @param onPress 按下回调，参数为frame坐标。按下时立即触发。
 * @param onMove 移动回调，参数为当前画布坐标。按住拖动时持续触发。
 * @param onRelease 抬起回调，参数为画布坐标。释放时立即触发。
 * @param onSecondaryClick 右键点击回调，参数为画布坐标。右键按下且抬起位置未发生明显偏移时触发。
 * @param onScroll 滚轮回调，参数为画布坐标和垂直滚动量。
 * @param onClick 点击手势回调，参数为画布坐标。短按释放后触发。仅左键触发。
 * @param onLongClick 长按手势回调，参数为画布坐标。按住超过系统阈值时触发。仅左键触发。
 * @param onSwipe 拖拽手势回调，参数为起止画布坐标。拖拽释放后触发。仅左键触发。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Composable
internal fun InputEffectLayer(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    rippleColor: Color = Color(0xCCFFFFFF),
    rippleDuration: Int = 500,
    trailColor: Color = Color(0xCCFFEB3B),
    trailWidth: Float = 3F,
    onPress: ((position: Offset) -> Unit)? = null,
    onMove: ((position: Offset) -> Unit)? = null,
    onRelease: ((position: Offset) -> Unit)? = null,
    onSecondaryClick: ((position: Offset) -> Unit)? = null,
    onScroll: ((position: Offset, delta: Float) -> Unit)? = null,
    onClick: ((position: Offset) -> Unit)? = null,
    onLongClick: ((position: Offset) -> Unit)? = null,
    onSwipe: ((start: Offset, end: Offset) -> Unit)? = null
) {
    // 使用 rememberUpdatedState 确保 pointerInput 协程始终读取最新的回调引用
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentOnSecondaryClick by rememberUpdatedState(onSecondaryClick)
    val currentOnScroll by rememberUpdatedState(onScroll)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    // 点击动效相关状态
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    val tapAnimAlpha = remember { Animatable(0F) }
    val tapAnimScope = rememberCoroutineScope()

    // 拖拽轨迹相关状态
    var swipeStart by remember { mutableStateOf<Offset?>(null) }
    var swipeEnd by remember { mutableStateOf<Offset?>(null) }


    Box(
        modifier = modifier
            // 回调控件的底层触控事件：仅转发左键（或触摸设备）产生的按下/移动/抬起坐标
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue

                        // 只转发左键的相关事件，不转发其他按键。
                        if (event.type == PointerEventType.Press ||
                            event.type == PointerEventType.Release
                        ) {
                            if (event.button != PointerButton.Primary) {
                                continue
                            }
                        } else if (event.type == PointerEventType.Move) {
                            if (change.pressed && !event.buttons.isPrimaryPressed) {
                                continue
                            }
                        }

                        when (event.type) {
                            PointerEventType.Press -> currentOnPress?.invoke(change.position)
                            PointerEventType.Move -> {
                                if (change.pressed) currentOnMove?.invoke(change.position)
                            }

                            PointerEventType.Release -> currentOnRelease?.invoke(change.position)
                            PointerEventType.Scroll -> {
                                val scrollDelta = change.scrollDelta.y
                                if (scrollDelta != 0F) {
                                    currentOnScroll?.invoke(change.position, scrollDelta)
                                }
                            }
                        }
                    }
                }
            }
            // 回调控件的点击与长按事件
            .pointerInput(Unit) {
                detectTapGestures(
                    matcher = PointerMatcher.Primary,
                    onTap = { position ->
                        currentOnClick?.invoke(position)

                        // 播放涟漪动效
                        tapPosition = position
                        tapAnimScope.launch {
                            tapAnimAlpha.snapTo(1F)
                            tapAnimAlpha.animateTo(0F, tween(rippleDuration))
                        }
                    },
                    onLongPress = { position ->
                        currentOnLongClick?.invoke(position)
                    }
                )
            }
            // 回调控件的拖拽事件
            .pointerInput(Unit) {
                detectDragGestures(
                    matcher = PointerMatcher.Primary,
                    onDragStart = { position ->
                        swipeStart = position
                        swipeEnd = position
                    },
                    onDrag = { dragAmount ->
                        /* 拖拽终点累加当前偏移量 */
                        val end = swipeEnd ?: return@detectDragGestures
                        // 如果坐标超出当前控件尺寸，则限制在控件尺寸之内。
                        val viewSize = size
                        val expect = end + dragAmount
                        var actual = expect
                        if (expect.x > viewSize.width) {
                            actual = actual.copy(x = viewSize.width.toFloat())
                        } else if (expect.x < 0) {
                            actual = actual.copy(x = 0F)
                        }
                        if (expect.y > viewSize.height) {
                            actual = actual.copy(y = viewSize.height.toFloat())
                        } else if (expect.y < 0) {
                            actual = actual.copy(y = 0F)
                        }

                        swipeEnd = actual
                    },
                    onDragEnd = {
                        val start = swipeStart
                        val end = swipeEnd
                        if (start != null && end != null) {
                            currentOnSwipe?.invoke(start, end)
                        }
                        swipeStart = null
                        swipeEnd = null
                    },
                    onDragCancel = {
                        swipeStart = null
                        swipeEnd = null
                    }
                )
            }
            // 回调鼠标右键点击事件
            .pointerInput(Unit) {
                detectTapGestures(
                    matcher = PointerMatcher.mouse(PointerButton.Secondary),
                    onTap = { position ->
                        currentOnSecondaryClick?.invoke(position)
                    }
                )
            }
    ) {
        // 如果未启用动效，不必绘制任何内容。
        if (!visible) {
            return@Box
        }

        /* 点击涟漪动效 */
        val tap = tapPosition
        if (tap != null && tapAnimAlpha.value > 0F) {
            Canvas(Modifier.fillMaxSize()) {
                // 涟漪扩散
                val progress = 1F - tapAnimAlpha.value
                val currentRadius = 50 * progress
                drawCircle(
                    color = rippleColor,
                    radius = currentRadius,
                    center = tap,
                    style = Stroke(width = 3F),
                    alpha = tapAnimAlpha.value
                )

                // 圆圈
                drawCircle(
                    color = rippleColor,
                    radius = 12F,
                    center = tap,
                    alpha = tapAnimAlpha.value
                )
            }
        }

        /* 拖拽轨迹动效 */
        val swipeS = swipeStart
        val swipeE = swipeEnd
        if (swipeS != null && swipeE != null) {
            Canvas(Modifier.fillMaxSize()) {
                // 轨迹线
                drawLine(
                    color = trailColor,
                    start = swipeS,
                    end = swipeE,
                    strokeWidth = trailWidth,
                    cap = StrokeCap.Round
                )

                // 起点圆
                drawCircle(
                    color = trailColor,
                    radius = 12F,
                    center = swipeS
                )

                // 终点圆
                drawCircle(
                    color = Color(0xCCFF5722),
                    radius = 12F,
                    center = swipeE
                )
            }
        }
    }
}


/**
 * 将控件坐标转换为视频帧坐标。
 *
 * @param[position] 输入坐标。
 * @param[canvasSize] 控件画布尺寸。
 * @param[frameSize] 视频帧尺寸。
 * @return 坐标。若输入坐标在视频帧之外，则返回空值。
 */
internal fun inputLayerToFramePosition(
    position: Offset,
    renderRect: Rect,
    frameSize: IntSize
): Offset {
    if (renderRect.width == 0F) {
        return position
    }

    val scale = frameSize.width / renderRect.width
    return position * scale
}
