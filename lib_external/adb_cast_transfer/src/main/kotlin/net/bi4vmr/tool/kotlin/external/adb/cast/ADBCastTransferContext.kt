package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import java.net.ServerSocket
import java.net.Socket

/**
 * 屏幕投射上下文（发送端）。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ADBCastTransferContext(

    /**
     * ADB 设备。
     */
    val device: LocalDevice,

    /**
     * 任务控制线程。
     */
    var taskThread: Thread? = null,

    /**
     * ServerSocket ： 监听接收端连接。
     */
    var remoteServer: ServerSocket? = null,

    /**
     * Socket ： 本地与接收端之间的视频通道。
     */
    var remoteVideoSocket: Socket? = null,

    /**
     * Socket ： 本地与接收端之间的音频通道。
     */
    var remoteAudioSocket: Socket? = null,

    /**
     * Socket ： 本地与接收端之间的控制通道。
     */
    var remoteControlSocket: Socket? = null,

    /**
     * ADB 转发端口。
     */
    var forwardPort: Int? = null,

    /**
     * 启动服务端的本地进程。
     */
    var serverProcess: Process? = null,

    /**
     * Socket ： 本地与 Android 设备之间的视频通道。
     */
    var videoSocket: Socket? = null,

    /**
     * 视频数据转发线程。
     */
    var videoForwardThread: Thread? = null,

    /**
     * Socket ： 本地与 Android 设备之间的音频通道。
     */
    var audioSocket: Socket? = null,

    /**
     * 音频数据转发线程。
     */
    var audioForwardThread: Thread? = null,

    /**
     * Socket ： 本地与 Android 设备之间的输入事件通道。
     */
    var controlSocket: Socket? = null,

    /**
     * 输入事件数据转发线程。
     */
    var controlForwardThread: Thread? = null,

    /**
     * 任务是否已被停止。
     *
     * 当用户终止投屏后应当置为 `true`，用于抑制资源清理引发的异常回调。
     */
    @Volatile
    var stopped: Boolean = false
)
