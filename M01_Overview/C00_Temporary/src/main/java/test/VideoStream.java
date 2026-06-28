package test;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

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
 *    [0..3]  flags（高位=1, 低位0=clientResized）
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
public class VideoStream {

    private static final String TAG = "VideoStream";

    // ── 协议常量（与 Streamer.java 和 demuxer.c 保持一致） ──
    private static final int  DEVICE_NAME_LENGTH = 64;
    private static final int  HEADER_SIZE        = 12;
    private static final long FLAG_CONFIG        = 1L << 62;
    private static final long FLAG_KEY_FRAME     = 1L << 61;
    private static final long PTS_MASK           = FLAG_KEY_FRAME - 1;

    // ── Codec ID（ASCII，对应 demuxer.c 中的宏定义） ──
    private static final int CODEC_ID_H264 = 0x68323634; // "h264"
    private static final int CODEC_ID_H265 = 0x68323635; // "h265"

    // ── FPS 统计间隔 ──
    private static final long FPS_LOG_INTERVAL_MS = 5_000;

    private final DataInputStream in;
    private final ScreenDisplay display;

    // FFmpeg 对象
    private AVCodecContext codecCtx;
    private AVPacket       avPacket;
    private AVFrame        yuvFrame;

    // 统计计数器
    private long    frameCount;
    private long    fpsWindowStart;
    private boolean firstFrameLogged;

    private volatile boolean running = true;

    public VideoStream(InputStream in, ScreenDisplay display) {
        this.in = new DataInputStream(in);
        this.display = display;
    }

    /**
     * 启动流接收与解码（阻塞，应在独立线程中调用）。
     * 对应 demuxer.c 中的 run_demuxer() 函数。
     */
    public void start() throws IOException {
        // 0. 读取并丢弃 dummy byte
        //    服务端在 tunnelForward 模式下会先发 1 字节 0x00 用于检测连接是否正常
        in.readByte();
        log("Handshake: dummy byte received");

        // 1. 读取设备名（64 字节）
        String deviceName = readDeviceName();
        log("Device name: " + deviceName);

        // 2. 读取 Codec ID（4 字节大端）
        int rawCodecId = in.readInt();
        int avCodecId = resolveCodecId(rawCodecId);
        log(String.format("Codec ID: 0x%08X (%s)", rawCodecId, codecIdName(rawCodecId)));

        // 3. 初始化 FFmpeg 解码器
        initDecoder(avCodecId);

        byte[] header = new byte[HEADER_SIZE];
        try {
            // 4. 读取首个会话包（分辨率信息）
            in.readFully(header);
            if (!isSessionPacket(header)) {
                throw new IOException("Expected session packet at stream start");
            }
            int[] wh = parseSessionPacket(header);
            log(String.format("Initial video size: %dx%d", wh[0], wh[1]));

            fpsWindowStart = System.currentTimeMillis();

            // 5. 主循环
            while (running) {
                in.readFully(header);

                if (isSessionPacket(header)) {
                    int[] newWh = parseSessionPacket(header);
                    log(String.format("Video resized: %dx%d", newWh[0], newWh[1]));
                } else {
                    long ptsFlags = readUInt64BE(header, 0);
                    int dataLen = readUInt32BE(header, 8);
                    boolean isConfig = (ptsFlags & FLAG_CONFIG) != 0;
                    boolean isKeyFrame = (ptsFlags & FLAG_KEY_FRAME) != 0;
                    long pts = isConfig ? AV_NOPTS_VALUE : (ptsFlags & PTS_MASK);

                    if (isConfig) {
                        log(String.format("Config packet received, size=%d bytes", dataLen));
                    }

                    byte[] data = new byte[dataLen];
                    in.readFully(data);
                    decodeAndDisplay(data, pts, isKeyFrame);
                }
            }
            log("Stream loop exited normally");
        } finally {
            releaseDecoder();
            log("Decoder released");
        }
    }

    public void stop() {
        log("Stop requested");
        running = false;
    }

    // ── 协议解析 ─────────────────────────────────────────────────────────────

    private String readDeviceName() throws IOException {
        byte[] buf = new byte[DEVICE_NAME_LENGTH];
        in.readFully(buf);
        int len = 0;
        while (len < buf.length && buf[len] != 0) len++;
        return new String(buf, 0, len, StandardCharsets.UTF_8);
    }

    private static boolean isSessionPacket(byte[] header) {
        return (header[0] & 0x80) != 0;
    }

    private static int[] parseSessionPacket(byte[] header) {
        return new int[]{readUInt32BE(header, 4), readUInt32BE(header, 8)};
    }

    private static int resolveCodecId(int raw) throws IOException {
        switch (raw) {
            case CODEC_ID_H264: return AV_CODEC_ID_H264;
            case CODEC_ID_H265: return AV_CODEC_ID_HEVC;
            default:
                throw new IOException(String.format("Unsupported codec: 0x%08X", raw));
        }
    }

    private static String codecIdName(int raw) {
        switch (raw) {
            case CODEC_ID_H264: return "H264";
            case CODEC_ID_H265: return "H265";
            default:            return "Unknown";
        }
    }

    // ── 解码器 ────────────────────────────────────────────────────────────────

    private void initDecoder(int avCodecId) throws IOException {
        AVCodec codec = avcodec_find_decoder(avCodecId);
        if (codec == null) throw new IOException("Decoder not found, avCodecId=" + avCodecId);

        String codecName = codec.long_name().getString();
        log("Using decoder: " + codecName);

        codecCtx = avcodec_alloc_context3(codec);
        if (codecCtx == null) throw new IOException("Failed to allocate codec context");

        codecCtx.flags(codecCtx.flags() | AV_CODEC_FLAG_LOW_DELAY);
        // FF_THREAD_FRAME = 1（FFmpeg 头文件定义，JavaCV 未导出该常量）
        codecCtx.thread_count(0);
        codecCtx.thread_type(1);

        if (avcodec_open2(codecCtx, codec, (org.bytedeco.ffmpeg.avutil.AVDictionary) null) < 0) {
            throw new IOException("Failed to open decoder: " + codecName);
        }
        log("Decoder initialized successfully");

        avPacket = av_packet_alloc();
        yuvFrame = av_frame_alloc();
    }

    private void decodeAndDisplay(byte[] data, long pts, boolean isKeyFrame) {
        av_new_packet(avPacket, data.length);
        avPacket.data().put(data);
        avPacket.size(data.length);
        avPacket.pts(pts);
        avPacket.dts(pts);
        if (isKeyFrame) avPacket.flags(avPacket.flags() | AV_PKT_FLAG_KEY);

        int ret = avcodec_send_packet(codecCtx, avPacket);
        av_packet_unref(avPacket);
        if (ret < 0) {
            System.err.printf("[%s] avcodec_send_packet failed, ret=%d%n", TAG, ret);
            return;
        }

        while (true) {
            ret = avcodec_receive_frame(codecCtx, yuvFrame);
            if (ret < 0) break;

            // 首帧到达时打印一次
            if (!firstFrameLogged) {
                log(String.format("First frame decoded: %dx%d, fmt=%d",
                        yuvFrame.width(), yuvFrame.height(), yuvFrame.format()));
                firstFrameLogged = true;
            }

            // 提取原始 YUV420P plane，直接传给 GPU 渲染（不做 CPU sws_scale）
            pushYuvToDisplay(yuvFrame);
            av_frame_unref(yuvFrame);

            // 周期性 FPS 统计
            frameCount++;
            long now = System.currentTimeMillis();
            long elapsed = now - fpsWindowStart;
            if (elapsed >= FPS_LOG_INTERVAL_MS) {
                double fps = frameCount * 1000.0 / elapsed;
                log(String.format("FPS: %.1f  (total frames: %d)", fps, frameCount));
                fpsWindowStart = now;
                frameCount = 0;
            }
        }
    }

    /**
     * 从 AVFrame 中提取三个 YUV420P plane，复制为 byte[] 后传给 ScreenDisplay 的 GPU 渲染。
     * stride（linesize）可能大于 width，OpenGL 通过 GL_UNPACK_ROW_LENGTH 处理 padding。
     */
    private void pushYuvToDisplay(AVFrame yuv) {
        int w = yuv.width(), h = yuv.height();
        if (w <= 0 || h <= 0) return;

        int yStride = yuv.linesize(0);
        int uStride = yuv.linesize(1);
        int vStride = yuv.linesize(2);

        byte[] yData = new byte[yStride * h];
        byte[] uData = new byte[uStride * (h / 2)];
        byte[] vData = new byte[vStride * (h / 2)];

        yuv.data(0).get(yData);
        yuv.data(1).get(uData);
        yuv.data(2).get(vData);

        display.updateYuvFrame(yData, yStride, uData, uStride, vData, vStride, w, h);
    }

    private void releaseDecoder() {
        if (yuvFrame != null) { av_frame_free(yuvFrame);        yuvFrame = null; }
        if (avPacket != null) { av_packet_free(avPacket);       avPacket = null; }
        if (codecCtx != null) { avcodec_free_context(codecCtx); codecCtx = null; }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", TAG, msg);
    }

    private static int readUInt32BE(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 24) | ((buf[off + 1] & 0xFF) << 16)
                | ((buf[off + 2] & 0xFF) << 8) | (buf[off + 3] & 0xFF);
    }

    private static long readUInt64BE(byte[] buf, int off) {
        return ((readUInt32BE(buf, off) & 0xFFFFFFFFL) << 32)
                | (readUInt32BE(buf, off + 4) & 0xFFFFFFFFL);
    }
}
