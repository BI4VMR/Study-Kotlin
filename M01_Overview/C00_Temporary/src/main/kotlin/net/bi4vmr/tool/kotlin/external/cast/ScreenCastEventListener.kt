package net.bi4vmr.tool.kotlin.external.cast

/**
 * 屏幕投射事件监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ScreenCastEventListener {

    fun onSizeChange(width: Int, height: Int)

    fun onNewFrame(yData: ByteArray, yStride: Int, uData: ByteArray, uStride: Int, vData: ByteArray, vStride: Int)

    fun onError(error: Exception)

    fun onTitleResolve(title: String) {
        // 可选，默认不进行任何操作。
    }
}
