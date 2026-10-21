package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import java.net.Socket

/**
 * 屏幕投射上下文。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class ADBCastContext(

    /* ----- 媒体处理相关组件 ----- */

    override val listener: ADBCastMediaListener,
    override val videoCodec: VideoCodec,
    override var videoSocket: Socket? = null,
    override var videoDecoder: VideoDecoder? = null,
    override val audioCodec: AudioCodec,
    override var audioSocket: Socket? = null,
    override var audioDecoder: AudioDecoder? = null,
    override var controlSocket: Socket? = null,
    override var inputController: InputController? = null,

    /**
     * ADB 设备。
     */
    val device: LocalDevice,

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
     * 视频解析线程。
     */
    var videoThread: Thread? = null,

    /**
     * 音频解析线程。
     */
    var audioThread: Thread? = null,

    /**
     * 进程结束时的清理线程。
     */
    var clearThread: Thread? = null,

    /**
     * 任务是否已被停止。
     *
     * 当用户终止投屏后应当置为 `true`，用于抑制资源清理引发的异常回调。
     */
    @Volatile
    var stopped: Boolean = false
) : ADBCastMediaContext
