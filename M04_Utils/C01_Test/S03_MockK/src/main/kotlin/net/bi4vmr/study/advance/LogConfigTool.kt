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
}
