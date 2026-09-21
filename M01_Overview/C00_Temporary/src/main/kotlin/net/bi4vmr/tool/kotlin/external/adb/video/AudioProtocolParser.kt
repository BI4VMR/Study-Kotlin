package net.bi4vmr.tool.kotlin.external.adb.video

import net.bi4vmr.tool.kotlin.external.adb.ScreenCastContext
import org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE
import java.io.DataInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频协议解析器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal object AudioProtocolParser {

    /**
     * 音频流解析与解码。
     *
     * <p>音频流没有设备名称与分辨率信息，握手阶段仅包含 4 字节的 Codec ID。</p>
     *
     * <p>协议格式（对应 Android 端 AudioEncoder.java 与 C 端 demuxer.c）：
     * <pre>
     * ── 握手 ──────────────────────────────────────────────
     *   4 bytes  Codec ID（大端），如 0x6f707573 = "opus"
     *
     * ── 每帧的 12 字节包头 ──────────────────────────────
     *   [0..7]  PTS + flags（大端 uint64）
     *             bit62 = CONFIG（OpusHead 等配置包）
     *             bit61 = KEY_FRAME
     *   [8..11] 数据长度（大端 uint32）
     *   <数据长度> 字节的编码数据
     * </pre>
     */
    fun parse(context: ScreenCastContext) {
        val socket = requireNotNull(context.audioSocket) { "Audio socket must exist!" }
        val stream = DataInputStream(socket.getInputStream())

        /* 读取音频编码类型 */
        val codecId = stream.readInt()
        if (codecId == 0) {
            // 服务端无法捕获音频，流被显式禁用，仅继续播放视频。
            println("Audio stream is explicitly disabled by the device!")
            return
        }
        if (codecId == 1) {
            throw IOException("Audio stream configuration error on the device!")
        }
        // 本工具在启动服务端时已指定编码，因此无需从协议中解析编码。


        /* 解析每帧数据 */
        val audioDecoder = requireNotNull(context.audioDecoder) { "Audio decoder must exist!" }

        val configFlag = 1L shl 62
        val keyFrameFlag = 1L shl 61
        val ptsMask = keyFrameFlag - 1

        val headerData = ByteArray(12)
        while (true) {
            stream.readFully(headerData)

            /* 媒体包 */
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
