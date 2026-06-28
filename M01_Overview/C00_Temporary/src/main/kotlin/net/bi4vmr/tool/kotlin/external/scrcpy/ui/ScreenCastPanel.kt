package net.bi4vmr.tool.kotlin.external.scrcpy.ui

import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastEventListener
import java.awt.BorderLayout
import java.nio.ByteBuffer
import java.nio.IntBuffer
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * 屏幕投射显示控件。
 *
 * 使用 JOGL (OpenGL) 渲染 YUV420P 视频帧：
 * - 解码线程将 YUV420P 三个 plane 的原始字节交给本类
 * - GL 线程通过 glTexImage2D 上传 Y/U/V 纹理
 * - GLSL Fragment Shader 在 GPU 完成 YUV→RGB 颜色转换（BT.601）
 *
 * 通过 [castListener] 属性获取 [ScreenCastEventListener] 实例，传入 ScreenCastManager 使用。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ScreenCastPanel : JPanel() {

    companion object {
        private const val TAG = "ScreenCastPanel"

        // BT.601 限制范围 YCbCr→RGB GLSL 片元着色器
        // 使用内置纹理坐标，与 GL2 固定管线兼容，无需额外 VBO。
        private val FRAGMENT_SHADER = """
            #version 120
            uniform sampler2D yTex;
            uniform sampler2D uTex;
            uniform sampler2D vTex;
            void main() {
                vec2 tc = gl_TexCoord[0].st;
                float y = texture2D(yTex, tc).r - 16.0/255.0;
                float u = texture2D(uTex, tc).r - 128.0/255.0;
                float v = texture2D(vTex, tc).r - 128.0/255.0;
                float r = clamp(1.164 * y + 1.596 * v,              0.0, 1.0);
                float g = clamp(1.164 * y - 0.391 * u - 0.813 * v,  0.0, 1.0);
                float b = clamp(1.164 * y + 2.018 * u,              0.0, 1.0);
                gl_FragColor = vec4(r, g, b, 1.0);
            }
        """.trimIndent()
    }

    /** 供外部嵌入宿主容器时直接访问 GLCanvas（如需额外监听器）。 */
    val glCanvas: GLCanvas

    /**
     * 投屏事件监听器实例，传入 [net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastManager.start] 使用。
     *
     * 内部实现接收分辨率变更和 YUV 帧数据，驱动 OpenGL 渲染。
     */
    val castListener: ScreenCastEventListener = CastEventHandler()

    // ── 帧数据（CastEventHandler 写，GlRenderer 读） ────────────────────────────

    private val frameLock = Any()
    private var yBuf: ByteBuffer? = null
    private var uBuf: ByteBuffer? = null
    private var vBuf: ByteBuffer? = null
    private var yStride = 0
    private var uStride = 0
    private var vStride = 0
    private var frameW = 0
    private var frameH = 0

    // 设备分辨率（CastEventHandler 写，GlRenderer 读；volatile 保证可见性）
    @Volatile private var deviceW = 0
    @Volatile private var deviceH = 0

    // 已接收帧数（用于首帧日志，仅在 CastEventHandler 里写，原子操作）
    private val frameCount = java.util.concurrent.atomic.AtomicLong(0)

    init {
        layout = BorderLayout()

        val profile = GLProfile.get(GLProfile.GL2)
        val caps = GLCapabilities(profile).apply { isDoubleBuffered = true }

        glCanvas = GLCanvas(caps)
        glCanvas.addGLEventListener(GlRenderer())
        add(glCanvas, BorderLayout.CENTER)
    }

    // ── ScreenCastEventListener 内部实现 ─────────────────────────────────────

    private inner class CastEventHandler : ScreenCastEventListener {

        override fun onSizeChange(width: Int, height: Int) {
            val isFirst = deviceW == 0 && deviceH == 0
            deviceW = width
            deviceH = height
            println("[$TAG] onSizeChange: ${width}x${height}  (isFirst=$isFirst)")

            if (isFirst) {
                SwingUtilities.invokeLater {
                    val initW = minOf(width, 540)
                    val initH = height * initW / width
                    val window = SwingUtilities.getWindowAncestor(this@ScreenCastPanel)
                    if (window == null) {
                        println("[$TAG] onSizeChange: parent window not found, skip resize")
                    } else {
                        window.setSize(initW, initH + (window.insets?.top ?: 0))
                        window.setLocationRelativeTo(null)
                        println("[$TAG] onSizeChange: window resized to ${window.width}x${window.height}")
                    }
                }
            }
        }

        override fun onNewFrame(
            yData: ByteArray, yStride: Int,
            uData: ByteArray, uStride: Int,
            vData: ByteArray, vStride: Int
        ) {
            val dw = deviceW
            val dh = deviceH
            if (dw <= 0 || dh <= 0) {
                println("[$TAG] onNewFrame: dropped, deviceSize not ready (${dw}x${dh})")
                return
            }

            synchronized(frameLock) {
                yBuf = ensureCapacity(yBuf, yData.size).also { it.clear(); it.put(yData); it.flip() }
                uBuf = ensureCapacity(uBuf, uData.size).also { it.clear(); it.put(uData); it.flip() }
                vBuf = ensureCapacity(vBuf, vData.size).also { it.clear(); it.put(vData); it.flip() }
                this@ScreenCastPanel.yStride = yStride
                this@ScreenCastPanel.uStride = uStride
                this@ScreenCastPanel.vStride = vStride
                frameW = dw
                frameH = dh
            }

            if (frameCount.getAndIncrement() == 0L) {
                println("[$TAG] onNewFrame: first frame  ySize=${yData.size} yStride=$yStride  device=${dw}x${dh}")
            }

            // canvas.display() 是线程安全的，可在解码线程直接调用以触发渲染
            glCanvas.display()
        }

        override fun onError(error: Exception) {
            System.err.println("[$TAG] onError: ${error::class.java.name}: ${error.message}")
            error.printStackTrace()
        }
    }

    // ── GLEventListener 内部实现 ──────────────────────────────────────────────

    private inner class GlRenderer : GLEventListener {

        // OpenGL 资源，仅在 GL 线程访问
        private var program = 0
        private val textures = IntArray(3) // [0]=Y  [1]=U  [2]=V
        private val texW = IntArray(3)
        private val texH = IntArray(3)

        // Letterbox viewport，仅在 GL 线程读写
        private var vpX = 0; private var vpY = 0
        private var vpW = 0; private var vpH = 0

        // GL 渲染线程已成功渲染的帧数（仅用于首帧日志）
        private var renderedCount = 0L

        override fun init(drawable: GLAutoDrawable) {
            val gl = drawable.gl.gL2
            gl.glClearColor(0f, 0f, 0f, 1f)

            program = buildProgram(gl)

            // 创建 Y/U/V 三个纹理对象并设置采样参数
            gl.glGenTextures(3, textures, 0)
            for (i in 0..2) {
                gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[i])
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR)
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR)
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE)
                gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE)
            }

            // 将纹理单元绑定到 Shader uniform
            gl.glUseProgram(program)
            gl.glUniform1i(gl.glGetUniformLocation(program, "yTex"), 0)
            gl.glUniform1i(gl.glGetUniformLocation(program, "uTex"), 1)
            gl.glUniform1i(gl.glGetUniformLocation(program, "vTex"), 2)

            println("[$TAG] GlRenderer.init: renderer=${gl.glGetString(GL2.GL_RENDERER)}  version=${gl.glGetString(GL2.GL_VERSION)}  program=$program")
        }

        override fun display(drawable: GLAutoDrawable) {
            val gl = drawable.gl.gL2
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT)

            // 快照帧数据，最小化持锁时间
            val y: ByteBuffer; val u: ByteBuffer; val v: ByteBuffer
            val ys: Int; val us: Int; val vs: Int
            val fw: Int; val fh: Int
            synchronized(frameLock) {
                if (yBuf == null) {
                    if (renderedCount == 0L) println("[$TAG] GlRenderer.display: yBuf not ready, skip")
                    return
                }
                y = yBuf!!; u = uBuf!!; v = vBuf!!
                ys = yStride; us = uStride; vs = vStride
                fw = frameW; fh = frameH
            }

            val dw = deviceW; val dh = deviceH
            if (dw > 0 && dh > 0) {
                recalcViewport(glCanvas.width, glCanvas.height, dw, dh)
                gl.glViewport(vpX, vpY, vpW, vpH)
            }

            // 单分量纹理，无对齐要求
            gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1)

            // 上传 Y 纹理（全分辨率；stride 可能大于 width，用 GL_UNPACK_ROW_LENGTH 跳过 padding）
            gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, ys)
            uploadTexture(gl, 0, textures[0], fw, fh, y)

            // 上传 U/V 纹理（YUV420P：宽高各为 Y 的一半）
            gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, us)
            uploadTexture(gl, 1, textures[1], fw / 2, fh / 2, u)

            gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, vs)
            uploadTexture(gl, 2, textures[2], fw / 2, fh / 2, v)

            gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0)
            gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4)

            // 全屏四边形；纹理 V 轴翻转以匹配图像行顺序
            gl.glUseProgram(program)
            gl.glBegin(GL2.GL_TRIANGLE_STRIP)
            gl.glTexCoord2f(0f, 1f); gl.glVertex2f(-1f, -1f)
            gl.glTexCoord2f(1f, 1f); gl.glVertex2f(1f, -1f)
            gl.glTexCoord2f(0f, 0f); gl.glVertex2f(-1f, 1f)
            gl.glTexCoord2f(1f, 0f); gl.glVertex2f(1f, 1f)
            gl.glEnd()

            if (renderedCount++ == 0L) {
                println("[$TAG] GlRenderer.display: first frame rendered  frameSize=${fw}x${fh}  viewport=${vpW}x${vpH}+${vpX}+${vpY}")
            }
        }

        override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
            println("[$TAG] GlRenderer.reshape: canvas=${width}x${height}")
            val dw = deviceW; val dh = deviceH
            if (dw > 0 && dh > 0) {
                recalcViewport(width, height, dw, dh)
                drawable.gl.gL2.glViewport(vpX, vpY, vpW, vpH)
                println("[$TAG] GlRenderer.reshape: viewport=${vpW}x${vpH}+${vpX}+${vpY}")
            } else {
                println("[$TAG] GlRenderer.reshape: device size not ready, skip viewport update")
            }
        }

        override fun dispose(drawable: GLAutoDrawable) {
            println("[$TAG] GlRenderer.dispose")
            val gl = drawable.gl.gL2
            if (program != 0) {
                gl.glDeleteProgram(program)
                program = 0
            }
            if (textures[0] != 0) gl.glDeleteTextures(3, textures, 0)
        }

        /**
         * 上传单个 YUV plane 至对应纹理。
         * 首次（或尺寸变化时）用 glTexImage2D 分配存储；后续用 glTexSubImage2D 更新（更高效）。
         */
        private fun uploadTexture(gl: GL2, idx: Int, tex: Int, w: Int, h: Int, data: ByteBuffer) {
            gl.glActiveTexture(GL2.GL_TEXTURE0 + idx)
            gl.glBindTexture(GL2.GL_TEXTURE_2D, tex)
            if (texW[idx] != w || texH[idx] != h) {
                gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE, w, h, 0, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_BYTE, data)
                texW[idx] = w; texH[idx] = h
            } else {
                gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, w, h, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_BYTE, data)
            }
        }

        private fun buildProgram(gl: GL2): Int {
            val frag = compileShader(gl, GL2.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            val prog = gl.glCreateProgram()
            gl.glAttachShader(prog, frag)
            gl.glLinkProgram(prog)
            gl.glDeleteShader(frag)

            val status = IntBuffer.allocate(1)
            gl.glGetProgramiv(prog, GL2.GL_LINK_STATUS, status)
            if (status[0] == GL2.GL_FALSE) {
                val len = IntBuffer.allocate(1)
                gl.glGetProgramiv(prog, GL2.GL_INFO_LOG_LENGTH, len)
                val logBytes = ByteArray(len[0])
                gl.glGetProgramInfoLog(prog, len[0], null, 0, logBytes, 0)
                System.err.println("[$TAG] Program link error: ${String(logBytes)}")
            }
            return prog
        }

        private fun compileShader(gl: GL2, type: Int, src: String): Int {
            val shader = gl.glCreateShader(type)
            gl.glShaderSource(shader, 1, arrayOf(src), null)
            gl.glCompileShader(shader)

            val status = IntBuffer.allocate(1)
            gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, status)
            if (status[0] == GL2.GL_FALSE) {
                val len = IntBuffer.allocate(1)
                gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, len)
                val logBytes = ByteArray(len[0])
                gl.glGetShaderInfoLog(shader, len[0], null, 0, logBytes, 0)
                System.err.println("[$TAG] Shader compile error: ${String(logBytes)}")
            }
            return shader
        }

        /** 计算 letterbox viewport：在 canvas 内居中保持设备宽高比。 */
        private fun recalcViewport(canvasW: Int, canvasH: Int, devW: Int, devH: Int) {
            val scale = minOf(canvasW.toDouble() / devW, canvasH.toDouble() / devH)
            vpW = (devW * scale).toInt()
            vpH = (devH * scale).toInt()
            vpX = (canvasW - vpW) / 2
            vpY = (canvasH - vpH) / 2
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private fun ensureCapacity(buf: ByteBuffer?, needed: Int): ByteBuffer =
        if (buf != null && buf.capacity() >= needed) buf else ByteBuffer.allocateDirect(needed)
}
