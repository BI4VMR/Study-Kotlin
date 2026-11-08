package net.bi4vmr.tool.kotlin.external.adb.common.callback

import java.io.File

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
     * 事件：总体传输开始。
     *
     * @param[count] 文件总数。
     * @param[size] 所有文件的总大小（字节）。
     */
    open fun onStart(count: Int, size: Long) {
        // 默认不作任何操作
    }

    /**
     * 事件：总体传输进度更新。
     *
     * @param[size] 所有文件已传输的数据量（字节）。
     * @param[progress] 总体进度（比例）。总大小为 `0` 时为 `NaN` 。
     */
    open fun onProgress(size: Long, progress: Double) {
        // 默认不作任何操作
    }

    /**
     * 事件：总体传输结束。
     *
     * @param[cancel] 传输是否被用户取消。
     */
    open fun onEnd(cancel: Boolean) {
        // 默认不作任何操作
    }

    /**
     * 事件：单个文件传输开始。
     *
     * @param[index] 文件序号（从 `0` 开始）。
     * @param[local] 源文件（PC 端）。
     * @param[remote] 目标路径（Android 端）。
     * @param[size] 文件大小（字节）。
     */
    open fun onFileStart(index: Int, local: File, remote: String, size: Long) {
        // 默认不作任何操作
    }

    /**
     * 事件：单个文件传输进度更新。
     *
     * @param[index] 文件序号（从 `0` 开始）。
     * @param[local] 源文件（PC 端）。
     * @param[size] 该文件已传输的数据量（字节）。
     * @param[progress] 该文件的进度（比例）。文件大小为 `0` 时为 `NaN` 。
     */
    open fun onFileProgress(index: Int, local: File, size: Long, progress: Double) {
        // 默认不作任何操作
    }

    /**
     * 事件：单个文件传输结束。
     *
     * @param[index] 文件序号（从 `0` 开始）。
     * @param[local] 源文件（PC 端）。
     * @param[cancel] 传输是否被用户取消。
     */
    open fun onFileEnd(index: Int, local: File, cancel: Boolean) {
        // 默认不作任何操作
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
