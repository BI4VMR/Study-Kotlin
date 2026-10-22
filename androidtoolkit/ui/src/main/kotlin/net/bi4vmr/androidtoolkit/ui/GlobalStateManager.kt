package net.bi4vmr.androidtoolkit.ui

import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.component.ToastManager

/**
 * 全局状态管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class GlobalStateManager {

    /**
     * 业务管理。
     */
    val logicManager = LogicManager

    /**
     * 全局窗口管理器。
     */
    val windowManager = WindowManager

    /**
     * 全局消息气泡管理器。
     */
    val toastManager = ToastManager()


    fun release() {
        logicManager.release()
    }
}
