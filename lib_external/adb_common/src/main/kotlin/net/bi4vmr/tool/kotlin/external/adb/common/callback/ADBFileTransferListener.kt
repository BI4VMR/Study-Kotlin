package net.bi4vmr.tool.kotlin.external.adb.common.callback

/**
 * ADB 文件传输监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
abstract class ADBFileTransferListener {

    // 任务取消标志位
    @Volatile
    private var cancelled: Boolean = false

    // 取消任务回调，由文件传输方法内部设置。
    private var cancelNotifier: (() -> Unit)? = null


    /**
     * 事件：传输开始。
     *
     * @param[size] 文件大小（字节）。
     */
    open fun onStart(size: Long) {
        // 默认不作任何操作，子类可根据需要重写。
    }

    /**
     * 事件：传输进度更新。
     *
     * @param[size] 已传输数据量（字节）。
     * @param[progress] 进度（比例）。值域： `[0, 1]` ；如果文件大小获取失败，则为 `NaN` 。
     */
    open fun onProgress(size: Long, progress: Double) {
        // 默认不作任何操作，子类可根据需要重写。
    }

    /**
     * 事件：传输结束。
     *
     * @param[cancel] 传输是否被用户取消。
     */
    open fun onEnd(cancel: Boolean) {
        // 默认不作任何操作，子类可根据需要重写。
    }

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
     * @return `true` 表示任务已取消；`false` 表示未取消。
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
