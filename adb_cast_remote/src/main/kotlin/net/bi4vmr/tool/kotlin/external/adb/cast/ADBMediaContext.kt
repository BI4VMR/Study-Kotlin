package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioDecoder
import net.bi4vmr.tool.kotlin.external.adb.cast.control.InputController
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.video.VideoDecoder
import java.net.Socket

/**
 * 媒体上下文。
 *
 * 解码器与协议解析器所需的最小资源集合，本地投屏与远程接收端均实现该接口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal interface ADBMediaContext {

    val listener: ADBCastEventListener

    val videoCodec: VideoCodec

    val audioCodec: AudioCodec

    var videoSocket: Socket?

    var videoDecoder: VideoDecoder?

    var audioSocket: Socket?

    var audioDecoder: AudioDecoder?

    var inputController: InputController?
}
