package net.bi4vmr.tool.kotlin.external.adb.cast.compose

import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastMediaListener
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import org.jetbrains.skia.Shader
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * ADB 投屏状态管理。
 *
 * 管理投屏任务并将事件回调转为 Flow ，以便 Compose 组件进行订阅。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
open class CastStateManager {

    /**
     * 投屏事件监听器。
     */
    protected val listener: ADBCastMediaListener = EventListener()

    /**
     * 投屏状态。
     */
    private val _stateFlow: MutableStateFlow<ADBCastState> = MutableStateFlow(ADBCastState.IDLE)

    /**
     * 投屏状态。
     */
    val stateFlow: StateFlow<ADBCastState> = _stateFlow

    /**
     * 当前画面的尺寸。
     */
    private val _frameSizeFlow: MutableStateFlow<IntSize> = MutableStateFlow(IntSize.Zero)

    /**
     * 当前画面的尺寸。
     */
    val frameSizeFlow: StateFlow<IntSize> = _frameSizeFlow

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
     * 初始化错误和视频错误信息。
     */
    private val _errorInfoFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * 初始化错误和视频错误信息。
     */
    val errorInfoFlow: StateFlow<Exception?> = _errorInfoFlow

    /**
     * 音频错误信息。
     */
    private val _audioErrorInfoFlow: MutableStateFlow<Exception?> = MutableStateFlow(null)

    /**
     * 音频错误信息。
     */
    val audioErrorInfoFlow: StateFlow<Exception?> = _audioErrorInfoFlow


    /**
     * 投屏事件监听器实现。
     */
    private inner class EventListener : ADBCastMediaListener {

        private val audioLine: SourceDataLine? by lazy {
            return@lazy try {
                val format = AudioFormat(
                    AudioDecoder.SAMPLE_RATE.toFloat(),
                    AudioDecoder.SAMPLE_BIT,
                    AudioDecoder.CHANNELS,
                    true,
                    false
                )
                AudioSystem.getSourceDataLine(format).apply {
                    open()
                    start()
                }
            } catch (e: Exception) {
                println("AudioSystem.getSourceDataLine error: ${e.message}")
                null
            }
        }


        override fun onConnectError(error: Exception) {
            _stateFlow.value = ADBCastState.ERROR
            _errorInfoFlow.value = error
        }

        override fun onVideoSizeChange(width: Int, height: Int) {
            _frameSizeFlow.value = IntSize(width, height)
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
            _stateFlow.value = ADBCastState.CASTING
        }

        override fun onVideoError(error: Exception) {
            _stateFlow.value = ADBCastState.ERROR
            _errorInfoFlow.value = error
        }

        override fun onAudioData(data: ByteArray) {
            audioLine?.write(data, 0, data.size)
        }

        override fun onAudioError(error: Exception) {
            _audioErrorInfoFlow.value = error
        }

        override fun onControlReady(controller: InputController) {
            _inputControllerFlow.value = controller
        }
    }
}
