package net.bi4vmr.tool.kotlin.external.cast.video

import net.bi4vmr.tool.kotlin.external.cast.ScreenCastContext
import org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

/**
 * 视频协议解析器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal object VideoProtocolParser {

    /**
     * 视频流解析与解码，对应 C 客户端的 demuxer.c + decoder.c。
     *
     * <p>协议格式（对应 Android 端 Streamer.java 与 C 端 demuxer.c）：
     * <pre>
     * ── 握手 ──────────────────────────────────────────────
     *  64 bytes  设备名（UTF-8, null-padded）
     *   4 bytes  Codec ID（大端），如 0x68323634 = "h264"
     *
     * ── 每帧的 12 字节包头 ──────────────────────────────
     *  若 header[0] & 0x80 == 1 → 会话包（分辨率信息）：
     *    [0..3]  flags（高位=01, 低位00=clientResized）
     *    [4..7]  视频宽度（大端 uint32）
     *    [8..11] 视频高度（大端 uint32）
     *
     *  若 header[0] & 0x80 == 0 → 媒体包：
     *    [0..7]  PTS + flags（大端 uint64）
     *              bit62 = CONFIG（SPS/PPS 配置包）
     *              bit61 = KEY_FRAME
     *    [8..11] 数据长度（大端 uint32）
     *    <数据长度> 字节的 H264/H265 裸码流
     * </pre>
     */
    fun parse(context: ScreenCastContext) {
        val listener = requireNotNull(context.listener) { "Listener must exist!" }
        val socket = requireNotNull(context.videoSocket) { "Video socket must exist!" }
        val stream = DataInputStream(socket.getInputStream())

        /*
         * 握手
         *
         * 服务端在TunnelForward模式下会先发送 `0x00` 用于握手。
         */
        val first = stream.readByte()
        if (first != 0.toByte()) {
            throw IOException("Unexpected handshake byte! We expect 0x00, but got [0x${first.toString(16)}].")
        }


        /* 读取设备名称 */
        val nameData = ByteArray(64)
        stream.readFully(nameData)
        // 截取数组中非0的部分
        val nameLength = nameData.indexOfFirst { it == 0.toByte() }
        val name = String(nameData, 0, nameLength, StandardCharsets.UTF_8)
        listener.onTitleResolve(name)


        /* 读取视频编码类型 */
        stream.readInt()
        // 本工具在启动服务端时已指定编码，因此无需从协议中解析编码。


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
                listener.onSizeChange(width, height)
            } else {
                /* 媒体包 */
                // 解析PTS和标志位
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
