package net.bi4vmr.tool.kotlin.external.adb

/**
 * 屏幕投射事件监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ScreenCastEventListener {

    /**
     * 事件：画面尺寸变化。
     *
     * 首次建立连接与设备旋转时将会触发。
     *
     * @param[width]  宽度。
     * @param[height] 高度。
     */
    fun onSizeChange(width: Int, height: Int)

    fun onNewFrame(yData: ByteArray, yStride: Int, uData: ByteArray, uStride: Int, vData: ByteArray, vStride: Int)

    /**
     * 音频数据回调。
     *
     * @param pcmData  S16 交错格式的 PCM 数据。
     * @param channels 声道数。
     * @param sampleRate 采样率。
     */
    fun onAudioData(pcmData: ByteArray, channels: Int, sampleRate: Int)

    /**
     * 事件：发生错误。
     */
    fun onError(error: Exception)

    /**
     * 事件：已解析标题。
     *
     * Scrcpy 服务端以设备型号作为标题，可用于界面显示。
     */
    fun onTitleResolve(title: String) {
        // 可选，默认不进行任何操作。
    }
}
