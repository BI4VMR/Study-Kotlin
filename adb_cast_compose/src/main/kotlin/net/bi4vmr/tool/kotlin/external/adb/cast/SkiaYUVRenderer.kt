package net.bi4vmr.tool.kotlin.external.adb.cast

import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Skia渲染器。
 *
 * 通过Skia引擎将YUV数据转换为RGB数据，Skia后端基于OpenGL/Vulkan，可以通过显卡硬件加速，与纯CPU转换方案相比更高效。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object SkiaYUVRenderer {

    /**
     * SkSL着色器。
     */
    private val SKSL_YUV_TO_RGB = """
            uniform shader yTex;
            uniform shader uTex;
            uniform shader vTex;
            uniform float2 uvScale;

            half4 main(float2 coord) {
                float y = yTex.eval(coord).r;
                float2 uvCoord = coord * uvScale;
                float u = uTex.eval(uvCoord).r;
                float v = vTex.eval(uvCoord).r;

                float r = y + 1.402 * (v - 0.5);
                float g = y - 0.344136 * (u - 0.5) - 0.714136 * (v - 0.5);
                float b = y + 1.772 * (u - 0.5);

                return half4(saturate(r), saturate(g), saturate(b), 1.0);
            }
        """.trimIndent()

    // 动态加载SKSL
    private val runtimeEffect: RuntimeEffect = RuntimeEffect.makeForShader(SKSL_YUV_TO_RGB)


    /**
     * 将 YUV(I420) 数据转换为 RGB Shader。
     *
     * @param[yData] Y 平面数据。
     * @param[yStride] Y 平面行跨度（字节数量）。
     * @param[uData] U 平面数据。
     * @param[uStride] U 平面行跨度。
     * @param[vData] V 平面数据。
     * @param[vStride] V 平面行跨度。
     * @param[width] 图像宽度。
     * @param[height] 图像高度。
     * @return Skia Shader。
     */
    fun getShader(
        yData: ByteArray,
        yStride: Int,
        uData: ByteArray,
        uStride: Int,
        vData: ByteArray,
        vStride: Int,
        width: Int,
        height: Int
    ): Shader {
        val uvWidth = (width + 1) / 2
        val uvHeight = (height + 1) / 2

        // Y 平面 Image（全分辨率，灰度）
        val yInfo = ImageInfo(width, height, ColorType.GRAY_8, ColorAlphaType.OPAQUE)
        val yImage = Image.makeRaster(yInfo, yData, yStride)

        // U 平面 Image（半分辨率，灰度）
        val uInfo = ImageInfo(uvWidth, uvHeight, ColorType.GRAY_8, ColorAlphaType.OPAQUE)
        val uImage = Image.makeRaster(uInfo, uData, uStride)

        // V 平面 Image（半分辨率，灰度）
        val vInfo = ImageInfo(uvWidth, uvHeight, ColorType.GRAY_8, ColorAlphaType.OPAQUE)
        val vImage = Image.makeRaster(vInfo, vData, vStride)

        // 构建Shader
        val yShader = yImage.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR)
        val uShader = uImage.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR)
        val vShader = vImage.makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.LINEAR)

        // 回收Image
        yImage.close()
        uImage.close()
        vImage.close()

        // 将全分辨率坐标映射到 UV 平面坐标空间
        // coord 范围是 [0, width) x [0, height)
        // UV 纹理范围是 [0, uvWidth) x [0, uvHeight)
        // W/H +1 用于防止整数除法默认向下取整，快速实现向上取整的效果。
        val uvScaleX = ((width + 1) / 2).toFloat() / width.toFloat()
        val uvScaleY = ((height + 1) / 2).toFloat() / height.toFloat()

        val uniformData = Data.makeFromBytes(floatsToBytes(floatArrayOf(uvScaleX, uvScaleY)))

        return runtimeEffect.makeShader(
            uniforms = uniformData,
            children = arrayOf(yShader, uShader, vShader),
            localMatrix = null
        )
    }

    private fun floatsToBytes(floats: FloatArray): ByteArray {
        // 每个Float变量占据4字节
        val bytes = ByteArray(floats.size * 4)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        floats.forEach { buffer.putFloat(it) }
        return bytes
    }
}
