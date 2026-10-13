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
     * 投屏事件监听器。
     */
    private val listener: ADBCastMediaListener = EventListener()

    /**
     * 投屏状态。
     */
    private val _stateFlow: MutableStateFlow<ADBCastState> = MutableStateFlow(ADBCastState.Loading)

    /**
     * 投屏状态。
     */
    val stateFlow: StateFlow<ADBCastState> = _stateFlow

    /**
     * 当前画面的尺寸。
     */
    private val _frameSizeFlow: MutableStateFlow<Pair<Int, Int>> = MutableStateFlow(Pair(0, 0))

    /**
     * 当前画面的尺寸。
     */
    val frameSizeFlow: StateFlow<Pair<Int, Int>> = _frameSizeFlow

    /**
     * 当前画面的 Shader 。
     */
    private val _shaderFlow: MutableStateFlow<Shader?> = MutableStateFlow(null)

    /**
     * 当前画面的 Shader 。
     */
    val shaderFlow: StateFlow<Shader?> = _shaderFlow

    /**
     * 输入控制器 。
     */
    private val _inputControllerFlow: MutableStateFlow<InputController?> = MutableStateFlow(null)

    /**
     * 输入控制器 。
     */
    val inputControllerFlow: StateFlow<InputController?> = _inputControllerFlow

    /**
     * 错误信息。
     */
    private val _errorInfoFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * 错误信息。
     */
    val errorInfoFlow: StateFlow<Exception?> = _errorInfoFlow


    fun start(device: ADBDevice, displayID: String = "0") {
        ADBCastManager.start(device, listener, displayID)
    }

    fun start(host: String, port: Int) {
        // TODO
    }

    fun stop() {
        ADBCastManager.stop(listener)
    }


    /**
     * 投屏事件监听器实现。
     */
    private inner class EventListener : ADBCastMediaListener {

        override fun onConnectError(error: Exception) {
            _stateFlow.value = ADBCastState.Error
            _errorInfoFlow.value = error
        }

        override fun onVideoSizeChange(width: Int, height: Int) {
            _frameSizeFlow.value = Pair(width, height)
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
            _shaderFlow.value = SkiaYUVRenderer.getShader(yData, yStride, uData, uStride, vData, vStride, width, height)
        }

        override fun onVideoReady() {
            _stateFlow.value = ADBCastState.Casting
        }

        override fun onVideoError(error: Exception) {
            _stateFlow.value = ADBCastState.Error
            _errorInfoFlow.value = error
        }

        override fun onAudioData(data: ByteArray) {
            // TODO
        }

        override fun onAudioError(error: Exception) {
            println(error)
        }

        override fun onControlReady(controller: InputController) {
            _inputControllerFlow.value = controller
        }
    }
}
