package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import java.net.Socket

/**
 * 屏幕投射上下文（接收端）。
 *
 * 每个发送端会话对应一个上下文，会话由视频、音频与控制三条 TCP 连接组成。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ADBRemoteReceiveContext(

    /**
     * 会话 ID 。
     */
    val sessionID: Long,

    /**
     * 事件监听器。
     */
    val listener: ADBCastEventListener,

    /**
     * 视频编码类型。
     */
    val videoCodec: VideoCodec,

    /**
     * 音频编码类型，未启用音频时为 `null` 。
     */
    val audioCodecOrNull: AudioCodec?,

    var videoSocket: Socket? = null,

    var videoDecoder: VideoDecoder? = null,

    var audioSocket: Socket? = null,

    var audioDecoder: AudioDecoder? = null,

    var controlSocket: Socket? = null,

    var inputController: InputController? = null,

    /**
     * 视频解析线程。
     */
    var videoThread: Thread? = null,

    /**
     * 音频解析线程。
     */
    var audioThread: Thread? = null,

    /**
     * 会话是否已被停止。
     */
    @Volatile
    var stopped: Boolean = false
)
