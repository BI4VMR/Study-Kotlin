package net.bi4vmr.tool.kotlin.external.adb

import net.bi4vmr.tool.kotlin.external.adb.control.InputController

/**
 * 屏幕投射事件监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBCastEventListener {

    /**
     * 事件回调：画面尺寸变化。
     *
     * 首次建立连接与设备旋转时将会触发。
     *
     * @param[width]  宽度。
     * @param[height] 高度。
     */
    fun onVideoSizeChange(width: Int, height: Int)

    /**
     * 事件回调：视频帧到达。
     *
     * 每一帧被解码后触发，数据格式为 YUV 。
     *
     * @param[yData] Y分量数据。
     * @param[yStride] Y分量行跨度（单位：字节）。
     * @param[uData] U分量数据。
     * @param[uStride] U分量行跨度（单位：字节）。
     * @param[vData] V分量数据。
     * @param[vStride] V分量行跨度（单位：字节）。
     * @param[width] 帧宽度。
     * @param[height] 帧高度。
     */
    fun onVideoFrame(
        yData: ByteArray,
        yStride: Int,
        uData: ByteArray,
        uStride: Int,
        vData: ByteArray,
        vStride: Int,
        width: Int,
        height: Int
    )

    /**
     * 事件回调：视频通道就绪。
     *
     * 当首帧到达时触发，界面层可以用来控制加载状态，发起投屏时显示加载动画，收到该回调时取消动画并显示内容。
     */
    fun onVideoReady() {
        // 可选，默认不进行任何操作。
    }

    /**
     * 事件回调：视频通道错误信息。
     *
     * @param[error] 异常信息。
     */
    fun onVideoError(error: Exception) {
        // 可选，默认不进行任何操作。
    }

    /**
     * 事件回调：音频通道错误信息。
     *
     * 每个音频数据包被解码后触发，数据格式为 PCM 。
     *
     * @param[error] 异常信息。
     */
    fun onAudioData(pcmData: ByteArray, channels: Int, sampleRate: Int) {
        // 可选，默认不进行任何操作。
    }

    /**
     * 事件回调：音频通道错误信息。
     *
     * @param[error] 异常信息。
     */
    fun onAudioError(error: Exception) {
        // 可选，默认不进行任何操作。
    }

    /**
     * 事件回调：远程控制通道就绪。
     *
     * @param[controller] 输入控制器实例。
     */
    fun onControlReady(controller: InputController) {
        // 可选，默认不进行任何操作。
    }

    /**
     * 事件回调：标题已解析。
     *
     * 视频通道在初始化时将发送设备型号，可以作为窗口标题使用。
     *
     * @param[title] 标题。
     */
    fun onTitleResolve(title: String) {
        // 可选，默认不进行任何操作。
    }
}
