package net.bi4vmr.tool.kotlin.external.adb

import net.bi4vmr.tool.kotlin.external.adb.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.video.VideoDecoder
import java.net.Socket

/**
 * 屏幕投射上下文。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ADBCastContext(

    /**
     * ADB 设备。
     */
    val device: ADBDevice,

    /**
     * 事件监听器。
     */
    val listener: ADBCastEventListener,

    /**
     * 屏幕 ID 。
     */
    val displayID: String,

    /**
     * 视频编码类型。
     */
    val videoCodec: VideoCodec,

    /**
     * 音频编码类型。
     */
    val audioCodec: AudioCodec,

    /**
     * 任务控制线程。
     */
    var taskThread: Thread? = null,

    /**
     * ADB 转发端口。
     */
    var forwardPort: Int? = null,

    /**
     * 启动服务端的本地进程。
     */
    var serverProcess: Process? = null,

    /**
     * Socket ：视频转发通道。
     */
    var videoSocket: Socket? = null,

    /**
     * 视频解码器。
     */
    var videoDecoder: VideoDecoder? = null,

    /**
     * Socket ：音频转发通道。
     */
    var audioSocket: Socket? = null,

    /**
     * 音频解码器。
     */
    var audioDecoder: AudioDecoder? = null,

    /**
     * 音频解析线程。
     */
    var audioThread: Thread? = null,

    /**
     * Socket ：控制转发通道。
     */
    var controlSocket: Socket? = null,

    /**
     * 输入控制器。
     */
    var inputController: InputController? = null,

    /**
     * 进程结束时的清理线程。
     */
    var clearThread: Thread? = null,

    /**
     * 服务端异常。
     *
     * 优先向上层汇报服务端异常，如果服务端已出现异常，则忽略后续的客户端线程终止等异常。
     */
    @Volatile
    var serverError: Exception? = null,

    /**
     * 任务是否已被停止。
     *
     * 当调用 [ScreenCastManager.stop] 后置为 `true`，用于抑制资源清理引发的异常回调。
     */
    @Volatile
    var stopped: Boolean = false
)
