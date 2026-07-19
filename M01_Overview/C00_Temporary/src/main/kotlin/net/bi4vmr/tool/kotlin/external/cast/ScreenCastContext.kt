package net.bi4vmr.tool.kotlin.external.cast

import net.bi4vmr.tool.kotlin.external.adb.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.DisplayInfo
import net.bi4vmr.tool.kotlin.external.cast.video.VideoDecoder
import java.net.Socket

/**
 * 屏幕投射上下文。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ScreenCastContext(

    /**
     * ADB设备。
     */
    val device: ADBDevice,

    /**
     * 事件监听器。
     */
    val listener: ScreenCastEventListener,

    /**
     * 屏幕信息。
     *
     * 空值表示使用默认屏幕。
     */
    val display: DisplayInfo?,

    /**
     * 视频编码类型。
     */
    val videoCodec: VideoCodec,

    /**
     * 任务控制线程。
     */
    var taskThread: Thread? = null,

    /**
     * ADB转发端口。
     */
    var forwardPort: Int? = null,

    /**
     * 启动服务端的本地进程。
     */
    var serverProcess: Process? = null,

    /**
     * Socket：视频转发通道。
     */
    var videoSocket: Socket? = null,

    /**
     * 视频解码器。
     */
    var videoDecoder: VideoDecoder? = null,

    /**
     * JVM关闭时的清理线程。
     */
    var clearThread: Thread? = null,
)
