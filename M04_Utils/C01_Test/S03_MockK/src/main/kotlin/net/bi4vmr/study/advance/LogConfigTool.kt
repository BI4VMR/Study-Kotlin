package net.bi4vmr.study.advance

import java.util.logging.Level


/**
 * 日志配置管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object LogConfigTool {

    private var listener: ConfigListener? = null

    /**
     * 注册回调。
     *
     * @param[l] 监听器实现。
     */
    fun addConfigListener(l: ConfigListener) {
        listener = l
    }

    /**
     * 回调接口：日志配置变更。
     */
    fun interface ConfigListener {

        /**
         * 回调方法：最小日志级别变更。
         *
         * @param[level] 日志级别。
         */
        fun onLevelChange(level: Level)
    }

    /**
     * 业务方法：准备日志目录。
     *
     * @param[onComplet] 准备完成回调。 `dir` 参数表示目录路径。
     */
    fun prepare(onComplet: (dir: String) -> Unit) {
        // 模拟耗时操作
        Thread.sleep(5000L)
        // 触发回调方法，通知调用者初始化完成。
        onComplet.invoke("/var/log/2025-12-31/")
    }
}
