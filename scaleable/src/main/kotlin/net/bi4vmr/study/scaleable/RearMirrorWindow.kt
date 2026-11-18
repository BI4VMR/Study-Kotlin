package net.bi4vmr.study.scaleable

import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Hosts [RearMirrorView] in a separate sub-window. The launcher window shows the wallpaper,
 * so its touches are duplicated to the wallpaper by InputDispatcher; a sub-window is not a
 * wallpaper target, so touches on the mirror are not forwarded to the wallpaper.
 */
class RearMirrorWindow(private val activity: Activity) {

    private val windowManager = activity.getSystemService(WindowManager::class.java)
    private var container: FrameLayout? = null
    private var mirrorView: RearMirrorView? = null

    val isShowing: Boolean get() = container != null

    fun show(displayId: Int) {
        if (isShowing) return
        val token = activity.window.decorView.windowToken ?: return
        val margin = DisplayUtils.dp2Px(24, activity).toInt()
        val padding = DisplayUtils.dp2Px(4, activity).toInt()
        val view = RearMirrorView(activity)
        val root = FrameLayout(activity).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(padding, padding, padding, padding)
            addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val lp = WindowManager.LayoutParams(
            DisplayUtils.dp2Px(640, activity).toInt(),
            DisplayUtils.dp2Px(360, activity).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.token = token
            gravity = Gravity.TOP or Gravity.END
            x = margin
            y = margin
            title = "RearMirror"
        }
        windowManager.addView(root, lp)
        container = root
        mirrorView = view
        view.start(displayId)
    }

    fun dismiss() {
        mirrorView?.stop()
        container?.let { runCatching { windowManager.removeViewImmediate(it) } }
        container = null
        mirrorView = null
    }
}
