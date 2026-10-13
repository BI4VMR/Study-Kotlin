package net.bi4vmr.tool.kotlin.external.adb.cast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice
import org.jetbrains.skia.Shader

/**
 * ADB 投屏状态管理。
 *
 * 管理投屏任务并将事件回调转为 Flow ，以便 Compose 组件进行订阅。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal class ADBCastStateManager {

    /**
     * 投屏事件监听器实现。
     */
    private val listener: ADBCastMediaListener = EventListener()

    /**
     * 投屏状态。
     */
    private val _state: MutableStateFlow<ADBCastState> = MutableStateFlow(ADBCastState.Loading)

    /**
     * 投屏状态。
     */
    val state: StateFlow<ADBCastState> = _state

    /**
     * 当前画面的尺寸。
     */
    private val _frameSize: MutableStateFlow<Pair<Int, Int>> = MutableStateFlow(Pair(0, 0))

    /**
     * 当前画面的尺寸。
     */
    val frameSize: StateFlow<Pair<Int, Int>> = _frameSize

    /**
     * 当前画面的 Shader 。
     */
    private val _shader: MutableStateFlow<Shader?> = MutableStateFlow(null)

    /**
     * 当前画面的 Shader 。
     */
    val shader: StateFlow<Shader?> = _shader


    fun start(device: ADBDevice, displayID: String = "0") {
        ADBCastManager.start(device, listener, displayID)
    }

    fun start(host: String, port: Int) {
        // ADBCastManager.start(device, listener, displayID)
    }

    fun stop() {

    }


    private inner class EventListener : ADBCastMediaListener {

        override fun onVideoSizeChange(width: Int, height: Int) {
            _frameSize.value = Pair(width, height)
        }

        override fun onVideoFrame(
            yData: ByteArray,
            yStride: Int,
            uData: ByteArray,
            uStride: Int,
            vData: ByteArray,
            vStride: Int,
            width: Int,
            height: Int
        ) {
            // 将 YUV (I420) 数据转换为 Skia Shader
            _shader.value = SkiaYUVRenderer.getShader(yData, yStride, uData, uStride, vData, vStride, width, height)
        }

        override fun onVideoReady() {
            _state.value = ADBCastState.Casting
        }

        override fun onControlReady(controller: InputController) {
            // inputController = controller
        }

        override fun onVideoError(error: Exception) {
            _state.value = ADBCastState.Error
            // errorInfo = "${error.javaClass.simpleName}: ${error.message}"
        }
    }
}
