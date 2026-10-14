package net.bi4vmr.tool.kotlin.external.adb.cast.video

import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastMediaContext
import net.bi4vmr.tool.kotlin.external.adb.cast.audio.AudioProtocolParser
import org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

/**
 * 视频协议解析器。
 *
 * Scrcpy 服务端编码逻辑在原工程的 `Streamer.java` 中；客户端解码逻辑在  `demuxer.c` 和 `decoder.c` 文件中。
 *
 * 客户端与服务端连接后，服务端首先发送一次设备名称与视频编码。
 *
 * - 设备名称为 64 字节，采用 UTF-8 编码，长度不足 64 字节的部分以 `0` 填充。
 * - 视频编码为 4 字节整型数值。
 *
 * 后续数据包为视频帧数据或控制信息，包头为 12 字节。
 *
 * 包头首字节最高位为 `1` 时表示会话包，第二字节为视频宽度（大端， `uint32` 格式），第三字节为视频高度（大端， `uint32` 格式）。
 *
 * 包头首字节最高位为 `0` 时表示媒体包，前 8 字节为 PTS（大端， `uint64` 格式），第 9 - 12 字节为数据长度（大端， `uint32` 格式），
 * 后续为指定编码的视频流。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object VideoProtocolParser {

    /**
     * 日志工具。
     */
    private val logger: Logger = LoggerFactory.getLogger(AudioProtocolParser::class.java)


    fun parse(context: ADBCastMediaContext) {
        val listener = requireNotNull(context.listener) { "Listener must exist!" }
        val socket = requireNotNull(context.videoSocket) { "Video socket must exist!" }
        val stream = DataInputStream(socket.getInputStream())

        /* 读取设备名称 */
        val nameData = ByteArray(64)
        stream.readFully(nameData)
        // 截取数组中非 0 的部分
        val nameLength = nameData.indexOfFirst { it == 0.toByte() }
        val name = String(nameData, 0, nameLength, StandardCharsets.UTF_8)
        listener.onTitleResolve(name)


        /* 读取视频编码类型 */
        val codecID = stream.readInt()
        // 本工具在启动服务端时已指定编码，因此无需从协议中解析编码。
        logger.info("Video codec ID is [0x{}].", codecID.toString(16))


        /* 解析每帧数据 */
        val videoDecoder = requireNotNull(context.videoDecoder) { "Video decoder must exist!" }

        val configFlag = 1L shl 62
        val keyFrameFlag = 1L shl 61
        val ptsMask = keyFrameFlag - 1

        val headerData = ByteArray(12)
        while (true) {
            stream.readFully(headerData)
            if (isSessionPacket(headerData)) {
                /* 会话包 */
                val width = ByteBuffer.wrap(headerData, 4, 4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .int
                val height = ByteBuffer.wrap(headerData, 8, 4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .int
                listener.onVideoSizeChange(width, height)
                context.inputController?.updateFrameSize(width, height)
            } else {
                /* 媒体包 */
                // 解析 PTS 和标志位
                val ptsAndFlags = ByteBuffer.wrap(headerData, 0, 8)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getLong()

                val config = (ptsAndFlags and configFlag) != 0L
                val keyFrame = (ptsAndFlags and keyFrameFlag) != 0L
                val pts = if (config) AV_NOPTS_VALUE else (ptsAndFlags and ptsMask)

                // 解析数据长度
                val dataLength = ByteBuffer.wrap(headerData, 8, 4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getInt()

                // 按长度读取媒体数据
                val datas = ByteArray(dataLength)
                stream.readFully(datas)
                videoDecoder.decode(datas, pts, keyFrame)
            }
        }
    }

    /**
     * 判断是否为会话包。
     *
     * 首字节的最高位为 `1` 时表示会话包，最高位为 `0` 表示媒体包。
     *
     * @param header 包头数据。
     * @return 若为会话包则返回 `true`，否则返回 `false`。
     */
    private fun isSessionPacket(header: ByteArray): Boolean = header[0] and 0x80.toByte() != 0.toByte()
}
