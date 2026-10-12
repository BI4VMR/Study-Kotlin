package net.bi4vmr.tool.kotlin.external.adb.cast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader

/**
 * Skia Shader显示层。
 *
 * 接收YUV信号对应的Shader并绘制到画布上。
 *
 * @param[frameSize] 视频帧尺寸。
 * @param[shader] Skia Shader。
 */
@Composable
internal fun SkiaShaderLayer(
    frameSize: IntSize,
    shader: Shader?
) {
    // 画布尺寸
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 有效内容区域
    val renderRect = remember(canvasSize, frameSize) {
        fitCanvas(canvasSize, frameSize)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        if (frameSize.width <= 0 || frameSize.height <= 0 || shader == null) {
            return@Canvas
        }

        val skiaCanvas = drawContext.canvas.skiaCanvas
        val skiaPaint = Paint().apply { this.shader = shader }

        // Skia Canvas是整个Compose体系共享的，所以绘制本画面前需保存状态，绘制结束后恢复状态，防止缩放操作影响到其他组件。
        skiaCanvas.save()

        // 变换画布，在缩放并居中后的位置绘制。
        skiaCanvas.translate(renderRect.left, renderRect.top)
        skiaCanvas.scale(
            renderRect.width / frameSize.width.toFloat(),
            renderRect.height / frameSize.height.toFloat()
        )

        /*
         * 绘制纹理。
         *
         * SkSL代码以 (0, 0) 为初始坐标，因此必须配合上述Canvas变换使画面移动到正确的位置。
         */
        skiaCanvas.drawRect(
            Rect.makeWH(frameSize.width.toFloat(), frameSize.height.toFloat()),
            skiaPaint
        )
        skiaPaint.close()

        skiaCanvas.restore()
    }
}


/**
 * 寻找绘制区域。
 *
 * 将视频帧长边缩放至画布尺寸并居中，返回该区域的坐标。
 *
 * @param canvasSize 画布尺寸。
 * @param frameSize 视频帧尺寸。
 * @return 目标区域坐标。
 */
internal fun fitCanvas(canvasSize: IntSize, frameSize: IntSize): Rect {
    if (canvasSize.width <= 0 || canvasSize.height <= 0 || frameSize.width <= 0 || frameSize.height <= 0) {
        return Rect(0F, 0F, 0F, 0F)
    }

    val canvasAspect = canvasSize.width.toFloat() / canvasSize.height
    val frameAspect = frameSize.width.toFloat() / frameSize.height

    val renderWidth: Float
    val renderHeight: Float
    if (canvasAspect > frameAspect) {
        // Canvas比Frame宽，高度限制为Canvas的高。
        renderHeight = canvasSize.height.toFloat()
        renderWidth = renderHeight * frameAspect
    } else if (canvasAspect < frameAspect) {
        // Canvas比Frame高，宽度限制为Canvas的宽。
        renderWidth = canvasSize.width.toFloat()
        renderHeight = renderWidth / frameAspect
    } else {
        // Canvas和Frame等比例，以Canvas尺寸为准。
        renderWidth = canvasSize.width.toFloat()
        renderHeight = canvasSize.height.toFloat()
    }

    val left = (canvasSize.width - renderWidth) / 2F
    val top = (canvasSize.height - renderHeight) / 2F

    return Rect.makeXYWH(left, top, renderWidth, renderHeight)
}
