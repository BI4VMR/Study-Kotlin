package net.bi4vmr.study.scaleable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Mirrors the rear display into this view via IWindowManager#mirrorDisplay and
 * injects touches back to the rear display. Requires platform signature.
 */
class RearMirrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    companion object {
        /** Override with: adb shell setprop persist.launcher.rear_display_id <id> */
        private const val PROP_REAR_DISPLAY_ID = "persist.launcher.rear_display_id"
        private const val INJECT_MODE_ASYNC = 0

        fun findRearDisplayId(context: Context): Int {
            return 5
        }
    }

    private var targetDisplayId = -1
    private var mirrorSc: SurfaceControl? = null
    private var srcWidth = 0
    private var srcHeight = 0
    private val touchMatrix = Matrix()

    init {
        holder.addCallback(this)
        setZOrderOnTop(false)
    }

    fun start(displayId: Int) {
        targetDisplayId = displayId
        if (holder.surface?.isValid == true) attachMirror()
    }

    fun stop() {
        detachMirror()
        targetDisplayId = -1
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (targetDisplayId >= 0) attachMirror()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        mirrorSc?.let { applyLayout(it, width, height) }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        detachMirror()
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun attachMirror() {
        detachMirror()
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(targetDisplayId)
        if (display == null) {
            Logger.e("rear display $targetDisplayId not found")
            return
        }
        val size = Point().also { @Suppress("DEPRECATION") display.getRealSize(it) }
        srcWidth = size.x
        srcHeight = size.y
        try {
            val wms = Class.forName("android.view.WindowManagerGlobal")
                .getMethod("getWindowManagerService").invoke(null)
            val sc = SurfaceControl::class.java.getDeclaredConstructor().newInstance()
            val ok = wms.javaClass
                .getMethod("mirrorDisplay", Int::class.javaPrimitiveType, SurfaceControl::class.java)
                .invoke(wms, targetDisplayId, sc) as Boolean
            if (!ok || !sc.isValid) {
                Logger.e("mirrorDisplay failed, displayId = $targetDisplayId")
                return
            }
            mirrorSc = sc
            SurfaceControl.Transaction()
                .reparent(sc, surfaceControl)
                .setVisibility(sc, true)
                .apply()
            applyLayout(sc, width, height)
            Logger.i("mirror attached, displayId = $targetDisplayId, src = ${srcWidth}x$srcHeight")
        } catch (e: Exception) {
            Logger.e("attachMirror error: $e")
        }
    }

    private fun applyLayout(sc: SurfaceControl, w: Int, h: Int) {
        if (srcWidth <= 0 || srcHeight <= 0 || w <= 0 || h <= 0) return
        val scale = minOf(w.toFloat() / srcWidth, h.toFloat() / srcHeight)
        val dx = (w - srcWidth * scale) / 2f
        val dy = (h - srcHeight * scale) / 2f
        SurfaceControl.Transaction()
            .setCrop(sc, Rect(0, 0, srcWidth, srcHeight))
            .setScale(sc, scale, scale)
            .setPosition(sc, dx, dy)
            .apply()
        // view coords -> rear display coords
        touchMatrix.reset()
        touchMatrix.postTranslate(-dx, -dy)
        touchMatrix.postScale(1f / scale, 1f / scale)
    }

    private fun detachMirror() {
        mirrorSc?.let {
            runCatching {
                SurfaceControl.Transaction().reparent(it, null).apply()
                it.release()
            }
        }
        mirrorSc = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (targetDisplayId < 0 || mirrorSc == null) return false
        val ev = MotionEvent.obtain(event)
        try {
            ev.transform(touchMatrix)
            ev.source = InputDevice.SOURCE_TOUCHSCREEN
            MotionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
                .invoke(ev, targetDisplayId)
            val im = context.getSystemService(InputManager::class.java)
            InputManager::class.java
                .getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)
                .invoke(im, ev, INJECT_MODE_ASYNC)
        } catch (e: Exception) {
            Logger.e("inject touch error: $e")
        } finally {
            ev.recycle()
        }
        return true
    }
}
