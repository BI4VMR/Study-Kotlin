package net.bi4vmr.study.advance

import java.util.logging.Level


/**
 * 回调接口：日志配置。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun interface LogConfigListener {

    /**
     * 回调方法：最小日志级别变更。
     *
     * @param[level] 执行结果。
     */
    fun onLevelChange(level: Level)
}
