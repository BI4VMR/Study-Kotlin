package net.bi4vmr.tool.kotlin.external.adb.common.callback

/**
 * ADB 目录传输监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
abstract class ADBFilesTransferListener {

    @Volatile
    private var cancelled: Boolean = false

    private var cancelNotifier: (() -> Unit)? = null


    /**
     * 取消传输任务。
     */
    fun notifyCancel() {
        val notifier = synchronized(this) {
            cancelled = true
            cancelNotifier
        }
        notifier?.invoke()
    }

    /**
     * 是否已发出取消指令。
     *
     * @return `true` 表示任务已取消； `false` 表示未取消。
     */
    fun isCancelled(): Boolean = cancelled

    /**
     * 设置取消指令监听器。
     *
     * 仅供文件传输方法内部使用，用户无需调用。
     */
    fun setCancelNotifier(cancelNotifier: () -> Unit) {
        val hasCancelled = synchronized(this) {
            this.cancelNotifier = cancelNotifier
            cancelled
        }

        // 注册时立即回调，防止用户先设置取消任务才开始导致无法取消。
        if (hasCancelled) {
            cancelNotifier.invoke()
        }
    }
}
