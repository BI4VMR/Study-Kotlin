package net.bi4vmr.tool.kotlin.external.adb.cast.audio

import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastMediaContext
import org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频协议解析器。
 *
 * Scrcpy 服务端编码逻辑在原工程的 `AudioEncoder.java` 中；客户端解码逻辑在  `demuxer.c` 文件中。
 *
 * 客户端与服务端连接后，服务端首先发送一次音频编码，数据为 4 字节整型数值。
 *
 * 后续数据包为音频数据或控制信息，包头为 12 字节。
 *
 * - [7 .. 0]  PTS 和标志位， `uint64` 格式。第 62 位为配置标志位；第 61 位为关键帧。
 * - [11 .. 8] 数据长度， `uint32` 格式。
 * - 音频数据，字节数与数据长度一致。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object AudioProtocolParser {

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(AudioProtocolParser::class.java)


    fun parse(context: ADBCastMediaContext) {
        val listener = requireNotNull(context.listener) { "Listener must exist!" }
        val socket = requireNotNull(context.audioSocket) { "Audio socket must exist!" }
        val stream = DataInputStream(socket.getInputStream())

        /* 读取音频编码类型 */
        val codecID = stream.readInt()
        // 本工具在启动服务端时已指定编码，因此无需从协议中解析编码。
        logger.info("Audio codec ID is [0x{}].", codecID.toString(16))

        if (codecID == 0) {
            /* 服务端音频捕获失败，此时设备通常会继续在本地外放音频。 */
            val info = "Audio stream is disabled by device!"
            logger.warn(info)
            listener.onAudioError(Exception(info))
            return
        } else if (codecID == 1) {
            /* 服务端音频配置失败 */
            val info = "Audio stream configuration error on the device!"
            logger.warn(info)
            listener.onAudioError(Exception(info))
            return
        }


        /* 解析每帧数据 */
        val audioDecoder = requireNotNull(context.audioDecoder) { "Audio decoder must exist!" }

        val configFlag = 1L shl 62
        val keyFrameFlag = 1L shl 61
        val ptsMask = keyFrameFlag - 1

        val headerData = ByteArray(12)
        while (true) {
            stream.readFully(headerData)

            // 解析PTS和标志位
            val ptsAndFlags = ByteBuffer.wrap(headerData, 0, 8)
                .order(ByteOrder.BIG_ENDIAN)
                .getLong()

            val config = (ptsAndFlags and configFlag) != 0L
            val pts = if (config) AV_NOPTS_VALUE else (ptsAndFlags and ptsMask)

            // 解析数据长度
            val dataLength = ByteBuffer.wrap(headerData, 8, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt()

            val datas = ByteArray(dataLength)
            stream.readFully(datas)
            audioDecoder.decode(datas, pts)
        }
    }
}
