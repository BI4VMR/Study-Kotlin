package test;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * 屏幕显示窗口，对应 C 客户端的 screen.c + texture.c。
 *
 * <p>使用 JOGL (OpenGL) 渲染视频帧：
 * <ul>
 *   <li>解码线程将 YUV420P 三个 plane 的原始字节交给本类</li>
 *   <li>GL 线程通过 {@code glTexImage2D} 上传 Y/U/V 纹理</li>
 *   <li>GLSL Fragment Shader 在 GPU 完成 YUV→RGB 颜色转换（BT.601）</li>
 * </ul>
 *
 * <p>这与 scrcpy C 客户端的 {@code SDL_UpdateYUVTexture} 方案完全等价，
 * 彻底消除了原方案中 CPU 端 {@code sws_scale} 的瓶颈。
 */
public class ScreenDisplay extends JFrame implements GLEventListener {

    private static final String TAG = "ScreenDisplay";

    // ── GLSL 着色器 ────────────────────────────────────────────────────────────
    // BT.601 限制范围（Android MediaCodec 默认输出） YCbCr→RGB 矩阵。
    // 使用 GL2 兼容模式内置纹理坐标，避免为固定矩形额外维护 VBO/顶点着色器。
    private static final String FRAGMENT_SHADER =
            "#version 120\n" +
            "uniform sampler2D yTex;\n" +
            "uniform sampler2D uTex;\n" +
            "uniform sampler2D vTex;\n" +
            "void main() {\n" +
            "    vec2 tc = gl_TexCoord[0].st;\n" +
            "    float y = texture2D(yTex, tc).r - 16.0/255.0;\n" +
            "    float u = texture2D(uTex, tc).r - 128.0/255.0;\n" +
            "    float v = texture2D(vTex, tc).r - 128.0/255.0;\n" +
            "    float r = clamp(1.164 * y + 1.596 * v,             0.0, 1.0);\n" +
            "    float g = clamp(1.164 * y - 0.391 * u - 0.813 * v, 0.0, 1.0);\n" +
            "    float b = clamp(1.164 * y + 2.018 * u,             0.0, 1.0);\n" +
            "    gl_FragColor = vec4(r, g, b, 1.0);\n" +
            "}\n";

    // ── UI 组件 ───────────────────────────────────────────────────────────────

    private final GLCanvas canvas;
    private InputController controller;

    // ── OpenGL 资源（仅 GL 线程访问） ──────────────────────────────────────────

    private int     program;
    private int[]   textures = new int[3]; // [0]=Y [1]=U [2]=V
    private int[]   texW     = new int[3]; // 已上传纹理的宽度（用于判断是否需要重新分配）
    private int[]   texH     = new int[3];

    // ── 帧数据（解码线程写入，GL 线程读取） ────────────────────────────────────

    private final Object frameLock = new Object();
    private ByteBuffer   yBuf, uBuf, vBuf;
    private int          yStride, uStride, vStride;
    private int          frameW, frameH;

    // 设备原始分辨率（用于鼠标坐标映射，volatile 供解码线程安全写入）
    private volatile int deviceW, deviceH;

    // GL viewport（letterbox 计算结果，仅 GL 线程读写）
    private int vpX, vpY, vpW, vpH;

    // ── 构造 ──────────────────────────────────────────────────────────────────

    public ScreenDisplay(String deviceName) {
        super("scrcpy - " + deviceName);

        GLProfile  profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(true);

        canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);

        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { handleMouse(e, 0); }
            @Override public void mouseReleased(MouseEvent e) { handleMouse(e, 1); }
            @Override public void mouseDragged(MouseEvent e)  { handleMouse(e, 2); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) { handleScroll(e); }
        };
        canvas.addMouseListener(adapter);
        canvas.addMouseMotionListener(adapter);
        canvas.addMouseWheelListener(adapter);

        // GLCanvas 继承自 AWT Canvas（非 Container），需用 JPanel 包裹后再设为 ContentPane
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.add(canvas, java.awt.BorderLayout.CENTER);
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void setInputController(InputController controller) {
        this.controller = controller;
    }

    /**
     * 由解码线程调用，传入原始 YUV420P 数据。
     * 不做任何颜色转换，直接缓存后触发 GL 渲染。
     *
     * @param yData   Y 平面字节数组
     * @param yStride Y 平面行步长（字节，含 padding）
     * @param uData   U 平面字节数组
     * @param uStride U 平面行步长
     * @param vData   V 平面字节数组
     * @param vStride V 平面行步长
     * @param w       图像宽度（像素）
     * @param h       图像高度（像素）
     */
    public void updateYuvFrame(byte[] yData, int yStride,
                               byte[] uData, int uStride,
                               byte[] vData, int vStride,
                               int w, int h) {
        // 首帧：调整窗口到合理的初始尺寸（等比缩小以适应屏幕）
        if (deviceW == 0) {
            int initW = Math.min(w, 540);
            int initH  = h * initW / w;
            SwingUtilities.invokeLater(() -> {
                setSize(initW, initH + getInsets().top);
                setLocationRelativeTo(null);
            });
        }

        synchronized (frameLock) {
            // 复用 DirectBuffer，避免 GC 压力
            yBuf = ensureCapacity(yBuf, yData.length);
            uBuf = ensureCapacity(uBuf, uData.length);
            vBuf = ensureCapacity(vBuf, vData.length);

            yBuf.clear(); yBuf.put(yData); yBuf.flip();
            uBuf.clear(); uBuf.put(uData); uBuf.flip();
            vBuf.clear(); vBuf.put(vData); vBuf.flip();

            this.yStride = yStride;
            this.uStride = uStride;
            this.vStride = vStride;
            this.frameW  = w;
            this.frameH  = h;
        }

        deviceW = w;
        deviceH = h;

        // 直接在调用线程（解码线程）触发 GL 渲染，canvas.display() 是线程安全的
        canvas.display();
    }

    // ── GLEventListener ───────────────────────────────────────────────────────

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0f, 0f, 0f, 1f);

        // 编译、链接着色器程序
        program = buildProgram(gl);

        // 创建 3 个纹理对象（Y / U / V），设置采样参数
        gl.glGenTextures(3, textures, 0);
        for (int i = 0; i < 3; i++) {
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[i]);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        }

        // 绑定纹理单元到着色器 uniform
        gl.glUseProgram(program);
        gl.glUniform1i(gl.glGetUniformLocation(program, "yTex"), 0);
        gl.glUniform1i(gl.glGetUniformLocation(program, "uTex"), 1);
        gl.glUniform1i(gl.glGetUniformLocation(program, "vTex"), 2);

        log("OpenGL init: renderer=" + gl.glGetString(GL2.GL_RENDERER)
                + "  version=" + gl.glGetString(GL2.GL_VERSION));
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

        // 快照帧数据（最小化持锁时间）
        ByteBuffer y, u, v;
        int ys, us, vs, fw, fh;
        synchronized (frameLock) {
            if (yBuf == null) return;
            y = yBuf; u = uBuf; v = vBuf;
            ys = yStride; us = uStride; vs = vStride;
            fw = frameW;  fh = frameH;
        }

        // 更新 viewport（当设备分辨率已知时进行 letterbox 计算）
        int dw = deviceW, dh = deviceH;
        if (dw > 0 && dh > 0) {
            recalcViewport(canvas.getWidth(), canvas.getHeight(), dw, dh);
            gl.glViewport(vpX, vpY, vpW, vpH);
        }

        // 单分量纹理：1 byte/pixel，无对齐要求
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);

        // 上传 Y 纹理（全分辨率，stride 可能 > width，用 GL_UNPACK_ROW_LENGTH 跳过 padding）
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, ys);
        uploadTexture(gl, 0, textures[0], fw, fh, y);

        // 上传 U/V 纹理（YUV420P：U/V 宽高各为 Y 的一半）
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, us);
        uploadTexture(gl, 1, textures[1], fw / 2, fh / 2, u);

        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, vs);
        uploadTexture(gl, 2, textures[2], fw / 2, fh / 2, v);

        // 复位 GL 状态
        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);

        // 渲染当前 viewport 内的全屏四边形；纹理 V 轴翻转以匹配图像行顺序。
        gl.glUseProgram(program);
        gl.glBegin(GL2.GL_TRIANGLE_STRIP);
        gl.glTexCoord2f(0f, 1f); gl.glVertex2f(-1f, -1f);
        gl.glTexCoord2f(1f, 1f); gl.glVertex2f( 1f, -1f);
        gl.glTexCoord2f(0f, 0f); gl.glVertex2f(-1f,  1f);
        gl.glTexCoord2f(1f, 0f); gl.glVertex2f( 1f,  1f);
        gl.glEnd();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        int dw = deviceW, dh = deviceH;
        if (dw > 0 && dh > 0) {
            recalcViewport(width, height, dw, dh);
            drawable.getGL().getGL2().glViewport(vpX, vpY, vpW, vpH);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        if (program  != 0) { gl.glDeleteProgram(program); program = 0; }
        if (textures[0] != 0) gl.glDeleteTextures(3, textures, 0);
    }

    // ── OpenGL 工具方法 ───────────────────────────────────────────────────────

    /**
     * 上传单个 YUV plane 到对应纹理。
     * 首次调用（或尺寸变化时）用 glTexImage2D 分配存储；后续用 glTexSubImage2D 更新数据（更高效）。
     */
    private void uploadTexture(GL2 gl, int idx, int tex, int w, int h, ByteBuffer data) {
        gl.glActiveTexture(GL2.GL_TEXTURE0 + idx);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, tex);
        if (texW[idx] != w || texH[idx] != h) {
            // 尺寸变化：重新分配纹理存储
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE,
                    w, h, 0, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_BYTE, data);
            texW[idx] = w;
            texH[idx] = h;
        } else {
            // 尺寸不变：只更新数据（更快）
            gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0,
                    w, h, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_BYTE, data);
        }
    }

    private int buildProgram(GL2 gl) {
        int frag = compileShader(gl, GL2.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int prog = gl.glCreateProgram();
        gl.glAttachShader(prog, frag);
        gl.glLinkProgram(prog);
        gl.glDeleteShader(frag);

        IntBuffer status = IntBuffer.allocate(1);
        gl.glGetProgramiv(prog, GL2.GL_LINK_STATUS, status);
        if (status.get(0) == GL2.GL_FALSE) {
            IntBuffer len = IntBuffer.allocate(1);
            gl.glGetProgramiv(prog, GL2.GL_INFO_LOG_LENGTH, len);
            byte[] logBytes = new byte[len.get(0)];
            gl.glGetProgramInfoLog(prog, len.get(0), null, 0, logBytes, 0);
            System.err.println("[ScreenDisplay] Program link error: " + new String(logBytes));
        }
        return prog;
    }

    private int compileShader(GL2 gl, int type, String src) {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[]{src}, null);
        gl.glCompileShader(shader);

        IntBuffer status = IntBuffer.allocate(1);
        gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, status);
        if (status.get(0) == GL2.GL_FALSE) {
            IntBuffer len = IntBuffer.allocate(1);
            gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, len);
            byte[] logBytes = new byte[len.get(0)];
            gl.glGetShaderInfoLog(shader, len.get(0), null, 0, logBytes, 0);
            System.err.println("[ScreenDisplay] Shader compile error: " + new String(logBytes));
        }
        return shader;
    }

    /** 计算 letterbox viewport：在 canvas 内居中保持设备宽高比。 */
    private void recalcViewport(int canvasW, int canvasH, int devW, int devH) {
        double scale = Math.min((double) canvasW / devW, (double) canvasH / devH);
        vpW = (int) (devW * scale);
        vpH = (int) (devH * scale);
        vpX = (canvasW - vpW) / 2;
        vpY = (canvasH - vpH) / 2;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer buf, int needed) {
        if (buf != null && buf.capacity() >= needed) return buf;
        return ByteBuffer.allocateDirect(needed);
    }

    // ── 鼠标事件 ──────────────────────────────────────────────────────────────

    private void handleMouse(MouseEvent e, int type) {
        if (controller == null) return;
        int[] dev = toDevice(e.getX(), e.getY());
        if (dev == null) return;
        try {
            if      (type == 0) controller.mouseDown(dev[0], dev[1], dev[2], dev[3]);
            else if (type == 1) controller.mouseUp  (dev[0], dev[1], dev[2], dev[3]);
            else                controller.mouseMove(dev[0], dev[1], dev[2], dev[3], true);
        } catch (IOException ex) {
            System.err.println("[ScreenDisplay] mouse error: " + ex.getMessage());
        }
    }

    private void handleScroll(MouseWheelEvent e) {
        if (controller == null) return;
        int[] dev = toDevice(e.getX(), e.getY());
        if (dev == null) return;
        float vscroll = (float) -e.getWheelRotation();
        try {
            controller.scroll(dev[0], dev[1], dev[2], dev[3], vscroll);
        } catch (IOException ex) {
            System.err.println("[ScreenDisplay] scroll error: " + ex.getMessage());
        }
    }

    /**
     * 将 canvas 内的鼠标坐标映射为设备原始坐标。
     * 考虑了 letterbox 偏移，超出图像区域时 clamp 而非返回 null。
     *
     * @return int[]{devX, devY, devW, devH}
     */
    private int[] toDevice(int canvasX, int canvasY) {
        int dw = deviceW, dh = deviceH;
        if (dw <= 0 || dh <= 0 || vpW <= 0 || vpH <= 0) return null;

        int imgX = Math.max(0, Math.min(vpW - 1, canvasX - vpX));
        int imgY = Math.max(0, Math.min(vpH - 1, canvasY - vpY));

        int devX = (int) (imgX * dw / (double) vpW);
        int devY = (int) (imgY * dh / (double) vpH);
        return new int[]{devX, devY, dw, dh};
    }

    private static void log(String msg) {
        System.out.printf("[%s] %s%n", TAG, msg);
    }
}
