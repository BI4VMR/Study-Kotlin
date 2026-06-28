package test;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 触控输入控制器，对应 C 客户端的 controller.c + control_msg.c。
 *
 * <p>通过控制 Socket 向 Android 设备发送触控事件。
 * 消息格式来自 control_msg.c 中的 sc_control_msg_serialize()。
 *
 * <p><b>INJECT_TOUCH_EVENT 包结构（32 字节）：</b>
 * <pre>
 * [0]      消息类型 = 0x02
 * [1]      action（0=DOWN, 1=UP, 2=MOVE）
 * [2..9]   pointer_id（int64 大端）：鼠标=-1(0xFFFFFFFFFFFFFFFF)
 * [10..13] 触控点 x（int32 大端，设备坐标）
 * [14..17] 触控点 y（int32 大端，设备坐标）
 * [18..19] 设备屏幕宽度（uint16 大端）
 * [20..21] 设备屏幕高度（uint16 大端）
 * [22..23] 压力（uint16 大端，0xFFFF=按下，0=抬起）
 * [24..27] action_button（uint32 大端，左键=0x00000001）
 * [28..31] buttons（uint32 大端，按下时=0x00000001，抬起时=0）
 * </pre>
 *
 * <p><b>INJECT_SCROLL_EVENT 包结构（21 字节）：</b>
 * <pre>
 * [0]      消息类型 = 0x03
 * [1..4]   触控点 x（int32 大端）
 * [5..8]   触控点 y（int32 大端）
 * [9..10]  设备屏幕宽度（uint16 大端）
 * [11..12] 设备屏幕高度（uint16 大端）
 * [13..14] hscroll（int16 大端，定点数，范围[-0x8000,0x7FFF]）
 * [15..16] vscroll（int16 大端，定点数，范围[-0x8000,0x7FFF]）
 * [17..20] buttons（uint32 大端）
 * </pre>
 */
public class InputController {

    // ── 消息类型（对应 control_msg.h 中的枚举） ──
    private static final byte TYPE_INJECT_TOUCH_EVENT  = 0x02;
    private static final byte TYPE_INJECT_SCROLL_EVENT = 0x03;

    // ── MotionEvent action（对应 Android AMOTION_EVENT_ACTION_*） ──
    private static final byte ACTION_DOWN = 0;
    private static final byte ACTION_UP   = 1;
    private static final byte ACTION_MOVE = 2;

    // ── Pointer ID（对应 control_msg.h 中的宏） ──
    // SC_POINTER_ID_MOUSE = UINT64_C(-1)，即 Java 中的 -1L
    private static final long POINTER_ID_MOUSE = -1L;

    // ── 鼠标左键（对应 AMOTION_EVENT_BUTTON_PRIMARY） ──
    private static final int BUTTON_PRIMARY = 0x00000001;

    // ── 压力定点数（对应 sc_float_to_u16fp(1.0f) = 0xFFFF） ──
    private static final int PRESSURE_PRESSED  = 0xFFFF;
    private static final int PRESSURE_RELEASED = 0x0000;

    private final OutputStream out;
    private final byte[] buf = new byte[32]; // 复用缓冲区，避免每次分配

    public InputController(OutputStream out) {
        this.out = out;
        System.out.println("[InputController] Ready");
    }

    // ── 公开 API ────────────────────────────────────────────────────────────

    /** 鼠标按下 */
    public synchronized void mouseDown(int devX, int devY, int devW, int devH) throws IOException {
        System.out.printf("[InputController] DOWN  (%4d, %4d) screen=%dx%d%n", devX, devY, devW, devH);
        sendTouchEvent(ACTION_DOWN, devX, devY, devW, devH, PRESSURE_PRESSED, BUTTON_PRIMARY, BUTTON_PRIMARY);
    }

    /** 鼠标移动（按住拖动） */
    public synchronized void mouseMove(int devX, int devY, int devW, int devH, boolean pressed) throws IOException {
        // MOVE 事件极为频繁，仅在 DEBUG 级别打印（生产中可注释掉）
        // System.out.printf("[InputController] MOVE  (%4d, %4d)%n", devX, devY);
        int buttons = pressed ? BUTTON_PRIMARY : 0;
        sendTouchEvent(ACTION_MOVE, devX, devY, devW, devH,
                pressed ? PRESSURE_PRESSED : PRESSURE_RELEASED, 0, buttons);
    }

    /** 鼠标抬起 */
    public synchronized void mouseUp(int devX, int devY, int devW, int devH) throws IOException {
        System.out.printf("[InputController] UP    (%4d, %4d) screen=%dx%d%n", devX, devY, devW, devH);
        sendTouchEvent(ACTION_UP, devX, devY, devW, devH, PRESSURE_RELEASED, BUTTON_PRIMARY, 0);
    }

    /** 鼠标滚轮（vscroll > 0 向上滚，< 0 向下滚） */
    public synchronized void scroll(int devX, int devY, int devW, int devH, float vscroll) throws IOException {
        System.out.printf("[InputController] SCROLL(%4d, %4d) v=%.1f%n", devX, devY, vscroll);
        sendScrollEvent(devX, devY, devW, devH, 0, vscroll);
    }

    // ── 序列化（对应 control_msg.c: sc_control_msg_serialize） ──────────────

    /**
     * 发送 INJECT_TOUCH_EVENT（32 字节）。
     * 字段顺序与 control_msg.c case SC_CONTROL_MSG_TYPE_INJECT_TOUCH_EVENT 完全一致。
     */
    private void sendTouchEvent(byte action, int x, int y, int screenW, int screenH,
                                int pressure, int actionButton, int buttons) throws IOException {
        buf[0]  = TYPE_INJECT_TOUCH_EVENT;
        buf[1]  = action;
        writeInt64BE(buf, 2,  POINTER_ID_MOUSE);  // pointer_id
        writeInt32BE(buf, 10, x);                  // position.point.x
        writeInt32BE(buf, 14, y);                  // position.point.y
        writeInt16BE(buf, 18, screenW);            // position.screen_size.width
        writeInt16BE(buf, 20, screenH);            // position.screen_size.height
        writeInt16BE(buf, 22, pressure);           // pressure (u16fp)
        writeInt32BE(buf, 24, actionButton);       // action_button
        writeInt32BE(buf, 28, buttons);            // buttons
        out.write(buf, 0, 32);
        out.flush();
    }

    /**
     * 发送 INJECT_SCROLL_EVENT（21 字节）。
     * 对应 control_msg.c case SC_CONTROL_MSG_TYPE_INJECT_SCROLL_EVENT。
     * scroll 值范围 [-16, 16]，内部归一化为 [-1, 1] 再转定点数。
     */
    private void sendScrollEvent(int x, int y, int screenW, int screenH,
                                 float hscroll, float vscroll) throws IOException {
        byte[] sbuf = new byte[21];
        sbuf[0] = TYPE_INJECT_SCROLL_EVENT;
        writeInt32BE(sbuf, 1,  x);
        writeInt32BE(sbuf, 5,  y);
        writeInt16BE(sbuf, 9,  screenW);
        writeInt16BE(sbuf, 11, screenH);
        writeInt16BE(sbuf, 13, floatToI16FP(Math.max(-1f, Math.min(1f, hscroll / 16f))));
        writeInt16BE(sbuf, 15, floatToI16FP(Math.max(-1f, Math.min(1f, vscroll / 16f))));
        writeInt32BE(sbuf, 17, 0); // buttons
        out.write(sbuf, 0, 21);
        out.flush();
    }

    // ── 编码工具（对应 util/binary.h 中的 sc_write*be / sc_float_to_i16fp） ──

    private static void writeInt64BE(byte[] b, int off, long v) {
        b[off]     = (byte) (v >> 56);
        b[off + 1] = (byte) (v >> 48);
        b[off + 2] = (byte) (v >> 40);
        b[off + 3] = (byte) (v >> 32);
        b[off + 4] = (byte) (v >> 24);
        b[off + 5] = (byte) (v >> 16);
        b[off + 6] = (byte) (v >>  8);
        b[off + 7] = (byte)  v;
    }

    private static void writeInt32BE(byte[] b, int off, int v) {
        b[off]     = (byte) (v >> 24);
        b[off + 1] = (byte) (v >> 16);
        b[off + 2] = (byte) (v >>  8);
        b[off + 3] = (byte)  v;
    }

    private static void writeInt16BE(byte[] b, int off, int v) {
        b[off]     = (byte) (v >> 8);
        b[off + 1] = (byte)  v;
    }

    /**
     * 将 [-1, 1] 范围的浮点数转为定点 int16（对应 sc_float_to_i16fp）。
     * 公式：i = f * 2^15，截断至 [-0x8000, 0x7FFF]
     */
    private static short floatToI16FP(float f) {
        int i = (int) (f * 0x8000);
        if (i > 0x7FFF) i = 0x7FFF;
        if (i < -0x8000) i = -0x8000;
        return (short) i;
    }
}
