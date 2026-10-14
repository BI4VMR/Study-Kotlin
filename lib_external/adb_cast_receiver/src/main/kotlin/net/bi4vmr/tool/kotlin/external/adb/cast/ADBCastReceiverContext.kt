package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import java.net.Socket

/**
 * 屏幕投射上下文（发送端）。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ADBCastReceiverContext(

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
    val deviceID: String,
) : ADBCastMediaContext

