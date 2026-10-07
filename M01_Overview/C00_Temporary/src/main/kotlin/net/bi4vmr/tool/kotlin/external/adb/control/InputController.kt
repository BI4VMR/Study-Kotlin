package net.bi4vmr.tool.kotlin.external.adb.control

import net.bi4vmr.tool.kotlin.external.adb.control.InputController.Companion.ACTION_DOWN
import net.bi4vmr.tool.kotlin.external.adb.control.InputController.Companion.ACTION_POINTER_DOWN
import net.bi4vmr.tool.kotlin.external.adb.control.InputController.Companion.ACTION_POINTER_UP
import net.bi4vmr.tool.kotlin.external.adb.control.InputController.Companion.ACTION_UP
import net.bi4vmr.tool.kotlin.external.adb.control.InputController.Companion.TYPE_BACK_OR_SCREEN_ON
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 触控输入控制器，对应 C 客户端的 controller.c + control_msg.c。
 *
 * 通过控制 Socket 向 Android 设备发送触控事件。
 * 消息格式来自 control_msg.c 中的 sc_control_msg_serialize()。
 *
 * **INJECT_TOUCH_EVENT 包结构（32 字节）：**
 * ```
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
 * ```
 *
 * **INJECT_SCROLL_EVENT 包结构（21 字节）：**
 * ```
 * [0]      消息类型 = 0x03
 * [1..4]   触控点 x（int32 大端）
 * [5..8]   触控点 y（int32 大端）
 * [9..10]  设备屏幕宽度（uint16 大端）
 * [11..12] 设备屏幕高度（uint16 大端）
 * [13..14] hscroll（int16 大端，定点数，范围[-0x8000,0x7FFF]）
 * [15..16] vscroll（int16 大端，定点数，范围[-0x8000,0x7FFF]）
 * [17..20] buttons（uint32 大端）
 * ```
 *
 * **INJECT_KEYCODE 包结构（14 字节）：**
 * ```
 * [0]      消息类型 = 0x00
 * [1]      action（0=DOWN, 1=UP）
 * [2..5]   keycode（int32 大端，Android KeyEvent.KEYCODE_*）
 * [6..9]   repeat（int32 大端，长按重复计数）
 * [10..13] metaState（int32 大端，Android KeyEvent.META_*）
 * ```
 *
 * **BACK_OR_SCREEN_ON 包结构（2 字节）：**
 * ```
 * [0]      消息类型 = 0x04
 * [1]      action（0=DOWN, 1=UP）
 * ```
 * 服务端行为：屏幕点亮时按下 BACK 键；屏幕熄灭时忽略 UP，在 DOWN 时改为唤醒屏幕（不会触发 BACK）。
 * scrcpy 桌面客户端默认将鼠标右键绑定至该消息（对应源码 cli.c 中 `mouse_bindings.pri.right_click = SC_MOUSE_BINDING_BACK`）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class InputController(private val ctrlStream: OutputStream) {

    companion object {
        // 消息类型（对应 control_msg.h 中的枚举）
        private const val TYPE_INJECT_KEYCODE: Byte = 0x00
        private const val TYPE_INJECT_TOUCH_EVENT: Byte = 0x02
        private const val TYPE_INJECT_SCROLL_EVENT: Byte = 0x03
        private const val TYPE_BACK_OR_SCREEN_ON: Byte = 0x04

        // MotionEvent action（对应 Android AMOTION_EVENT_ACTION_*）
        private const val ACTION_DOWN: Byte = 0
        private const val ACTION_UP: Byte = 1
        private const val ACTION_MOVE: Byte = 2
        private const val ACTION_POINTER_DOWN: Byte = 5  // 第 N 根手指按下（N>1）
        private const val ACTION_POINTER_UP: Byte = 6    // 中间手指抬起（非最后一个）

        // Pointer ID：SC_POINTER_ID_MOUSE = UINT64_C(-1)
        private const val POINTER_ID_MOUSE: Long = -1L

        // 鼠标左键（对应 AMOTION_EVENT_BUTTON_PRIMARY）
        private const val BUTTON_PRIMARY: Int = 0x00000001

        // 压力定点数（对应 sc_float_to_u16fp(1.0f) = 0xFFFF）
        private const val PRESSURE_PRESSED: Short = 0xFFFF.toShort()
        private const val PRESSURE_RELEASED: Short = 0x0000

        // Android按键码（对应 android/keycodes.h 中的 AKEYCODE_*）
        private const val KEYCODE_HOME: Int = 3
        private const val KEYCODE_APP_SWITCH: Int = 187
    }

    @Volatile
    private var frameWidth: Int = -1

    @Volatile
    private var frameHeight: Int = -1

    // 复用缓冲区，避免每次分配
    private val touchBuf = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
    private val scrollBuf = ByteBuffer.allocate(21).order(ByteOrder.BIG_ENDIAN)
    private val keycodeBuf = ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN)
    private val backBuf = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)

    /**
     * 更新帧的尺寸。
     *
     * 模拟触控时输入坐标以帧的尺寸为基准，因此该尺寸需由视频通道更新。
     *
     * @param[width] 帧的宽度。
     * @param[height] 帧的高度。
     */
    internal fun updateFrameSize(width: Int, height: Int) {
        frameWidth = width
        frameHeight = height
    }

    /** 鼠标按下 */
    fun mouseDown(x: Int, y: Int) {
        sendTouchEvent(ACTION_DOWN, x, y, PRESSURE_PRESSED, BUTTON_PRIMARY, BUTTON_PRIMARY)
    }

    /** 鼠标移动（按住拖动） */
    fun mouseMove(x: Int, y: Int, pressed: Boolean) {
        val buttons = if (pressed) BUTTON_PRIMARY else 0
        val pressure = if (pressed) PRESSURE_PRESSED else PRESSURE_RELEASED
        sendTouchEvent(ACTION_MOVE, x, y, pressure, 0, buttons)
    }

    /** 鼠标抬起 */
    fun mouseUp(x: Int, y: Int) {
        sendTouchEvent(ACTION_UP, x, y, PRESSURE_RELEASED, BUTTON_PRIMARY, 0)
    }

    /**
     * 多点触控：手指按下。
     *
     * 第一根手指（[pointerId]=0）使用 [ACTION_DOWN]，后续手指使用 [ACTION_POINTER_DOWN]。
     *
     * @param pointerId 手指 ID，从 0 开始递增，同一手势中每根手指 ID 唯一。
     */
    fun touchDown(pointerId: Int, devX: Int, devY: Int, devW: Int, devH: Int) {
        val action = if (pointerId == 0) ACTION_DOWN else ACTION_POINTER_DOWN
        sendTouchEvent(action, devX, devY, PRESSURE_PRESSED, 0, 0, pointerId.toLong())
    }

    /** 多点触控：手指移动。 */
    fun touchMove(pointerId: Int, devX: Int, devY: Int, devW: Int, devH: Int) {
        sendTouchEvent(ACTION_MOVE, devX, devY, PRESSURE_PRESSED, 0, 0, pointerId.toLong())
    }

    /**
     * 多点触控：手指抬起。
     *
     * 最后一根手指（[isLast]=true）使用 [ACTION_UP]，中间手指使用 [ACTION_POINTER_UP]。
     *
     * @param isLast 是否为最后一根抬起的手指。
     */
    fun touchUp(pointerId: Int, devX: Int, devY: Int, devW: Int, devH: Int, isLast: Boolean = false) {
        val action = if (isLast) ACTION_UP else ACTION_POINTER_UP
        sendTouchEvent(action, devX, devY, PRESSURE_RELEASED, 0, 0, pointerId.toLong())
    }

    /** 鼠标滚轮（vscroll > 0 向上滚，< 0 向下滚） */
    fun scroll(x: Int, y: Int, vscroll: Float) {
        sendScrollEvent(x, y, 0F, vscroll)
    }

    /**
     * 按下并释放返回键（BACK）。
     *
     * 使用 [TYPE_BACK_OR_SCREEN_ON] 而非通用按键注入：屏幕点亮时行为等同于 BACK 键；
     * 若设备屏幕处于熄灭状态，则改为唤醒屏幕而不会触发 BACK，避免熄屏后误触发返回逻辑。
     * 这也是 scrcpy 桌面客户端中鼠标右键的默认行为。
     */
    fun pressBack() {
        sendBackOrScreenOn(ACTION_DOWN)
        sendBackOrScreenOn(ACTION_UP)
    }

    /** 按下并释放主屏幕键（HOME）。 */
    fun pressHome() {
        pressReleaseKeycode(KEYCODE_HOME)
    }

    /** 按下并释放最近任务键（APP_SWITCH / 概览）。 */
    fun pressAppSwitch() {
        pressReleaseKeycode(KEYCODE_APP_SWITCH)
    }

    /**
     * 按下并释放任意 Android 按键码。
     *
     * @param keycode Android `KeyEvent.KEYCODE_*` 常量值。
     */
    fun pressReleaseKeycode(keycode: Int) {
        sendKeycodeEvent(ACTION_DOWN, keycode)
        sendKeycodeEvent(ACTION_UP, keycode)
    }

    // ── 序列化（对应 control_msg.c: sc_control_msg_serialize） ──

    /**
     * 发送 INJECT_TOUCH_EVENT（32 字节）。
     * 字段顺序与 control_msg.c case SC_CONTROL_MSG_TYPE_INJECT_TOUCH_EVENT 完全一致。
     */
    private fun sendTouchEvent(
        action: Byte,
        x: Int,
        y: Int,
        pressure: Short,
        actionButton: Int,
        buttons: Int,
        pointerId: Long = POINTER_ID_MOUSE
    ) {
        touchBuf.clear()
        touchBuf.put(TYPE_INJECT_TOUCH_EVENT)   // [0]
        touchBuf.put(action)                    // [1]
        touchBuf.putLong(pointerId)             // [2..9]
        touchBuf.putInt(x)                      // [10..13]
        touchBuf.putInt(y)                      // [14..17]
        touchBuf.putShort(frameWidth.toShort())    // [18..19]
        touchBuf.putShort(frameHeight.toShort())    // [20..21]
        touchBuf.putShort(pressure)             // [22..23]
        touchBuf.putInt(actionButton)           // [24..27]
        touchBuf.putInt(buttons)                // [28..31]

        ctrlStream.write(touchBuf.array(), 0, 32)
        ctrlStream.flush()
    }

    /**
     * 发送 INJECT_SCROLL_EVENT（21 字节）。
     * 对应 control_msg.c case SC_CONTROL_MSG_TYPE_INJECT_SCROLL_EVENT。
     * scroll 值范围 [-16, 16]，内部归一化为 [-1, 1] 再转定点数。
     */
    private fun sendScrollEvent(
        x: Int,
        y: Int,
        hscroll: Float,
        vscroll: Float
    ) {
        scrollBuf.clear()
        scrollBuf.put(TYPE_INJECT_SCROLL_EVENT) // [0]
        scrollBuf.putInt(x)                     // [1..4]
        scrollBuf.putInt(y)                     // [5..8]
        scrollBuf.putShort(frameWidth.toShort())   // [9..10]
        scrollBuf.putShort(frameHeight.toShort())   // [11..12]
        scrollBuf.putShort(floatToI16FP((hscroll / 16f).coerceIn(-1f, 1f)))  // [13..14]
        scrollBuf.putShort(floatToI16FP((vscroll / 16f).coerceIn(-1f, 1f)))  // [15..16]
        scrollBuf.putInt(0)                     // [17..20] buttons

        ctrlStream.write(scrollBuf.array(), 0, 21)
        ctrlStream.flush()
    }

    /**
     * 发送 INJECT_KEYCODE（14 字节）。
     * 对应 control_msg.c case SC_CONTROL_MSG_TYPE_INJECT_KEYCODE。
     * repeat 与 metaState 固定为 0，当前不支持组合键与长按重复计数。
     */
    private fun sendKeycodeEvent(action: Byte, keycode: Int) {
        keycodeBuf.clear()
        keycodeBuf.put(TYPE_INJECT_KEYCODE)  // [0]
        keycodeBuf.put(action)               // [1]
        keycodeBuf.putInt(keycode)           // [2..5]
        keycodeBuf.putInt(0)                 // [6..9]  repeat
        keycodeBuf.putInt(0)                 // [10..13] metaState

        ctrlStream.write(keycodeBuf.array(), 0, 14)
        ctrlStream.flush()
    }

    /**
     * 发送 BACK_OR_SCREEN_ON（2 字节）。
     * 对应 control_msg.c case SC_CONTROL_MSG_TYPE_BACK_OR_SCREEN_ON。
     */
    private fun sendBackOrScreenOn(action: Byte) {
        backBuf.clear()
        backBuf.put(TYPE_BACK_OR_SCREEN_ON) // [0]
        backBuf.put(action)                 // [1]

        ctrlStream.write(backBuf.array(), 0, 2)
        ctrlStream.flush()
    }

    // ── 编码工具（对应 util/binary.h 中的 sc_float_to_i16fp） ──

    /**
     * 将 [-1, 1] 范围的浮点数转为定点 int16（对应 sc_float_to_i16fp）。
     * 公式：i = f * 2^15，截断至 [-0x8000, 0x7FFF]
     */
    private fun floatToI16FP(f: Float): Short {
        return (f * 0x8000).toInt().coerceIn(-0x8000, 0x7FFF).toShort()
    }
}
