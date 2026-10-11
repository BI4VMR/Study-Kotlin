package net.bi4vmr.tool.kotlin.external.adb

import net.bi4vmr.tool.kotlin.external.adb.cast.common.ADBCastEventListener
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice
import java.net.Socket

/**
 * 屏幕投射上下文（发送端）。
 *
 * 发送端连接 ADB 设备，并将音视频数据转发至接收端，同时将接收端的控制数据转发至设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ADBRemoteCastContext(

    /**
     * ADB 设备。
     */
    val device: ADBDevice,

    /**
     * 事件监听器，发送端仅使用其错误回调。
     */
    val listener: ADBCastEventListener,

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
     * Socket ：设备视频通道。
     */
    var deviceVideoSocket: Socket? = null,

    /**
     * Socket ：设备音频通道。
     */
    var deviceAudioSocket: Socket? = null,

    /**
     * Socket ：设备控制通道。
     */
    var deviceControlSocket: Socket? = null,

    /**
     * Socket ：接收端视频通道。
     */
    var remoteVideoSocket: Socket? = null,

    /**
     * Socket ：接收端音频通道。
     */
    var remoteAudioSocket: Socket? = null,

    /**
     * Socket ：接收端控制通道。
     */
    var remoteControlSocket: Socket? = null,

    /**
     * 进程结束时的清理线程。
     */
    var clearThread: Thread? = null,

    /**
     * 服务端异常。
     */
    @Volatile
    var serverError: Exception? = null,

    /**
     * 任务是否已被停止。
     */
    @Volatile
    var stopped: Boolean = false
)
