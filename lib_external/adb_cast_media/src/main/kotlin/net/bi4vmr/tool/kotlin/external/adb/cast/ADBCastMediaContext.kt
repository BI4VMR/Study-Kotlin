package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import java.net.Socket

/**
 * 屏幕投射媒体上下文。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBCastMediaContext {

    /**
     * 事件监听器。
     */
    val listener: ADBCastMediaListener

    /**
     * 视频编码类型。
     */
    val videoCodec: VideoCodec

    /**
     * Socket ：视频转发通道。
     */
    var videoSocket: Socket?

    /**
     * 视频解码器。
     */
    var videoDecoder: VideoDecoder?

    /**
     * 音频编码类型。
     */
    val audioCodec: AudioCodec

    /**
     * Socket ：音频转发通道。
     */
    var audioSocket: Socket?

    /**
     * 音频解码器。
     */
    var audioDecoder: AudioDecoder?

    /**
     * Socket ：控制转发通道。
     */
    var controlSocket: Socket?

    /**
     * 输入控制器。
     */
    var inputController: InputController?
}
