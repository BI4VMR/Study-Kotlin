package net.bi4vmr.study.advance

import java.util.logging.Level

/**
 * 日志工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class LogManager {

    var minLevel: Level = Level.INFO
        private set

    init {
        // 注册配置变更监听器，并同步设置最小级别。
        LogConfigTool.addConfigListener { newLevel ->
            minLevel = newLevel
        }
    }

    // 业务方法：导出日志
    fun saveLog(callback: StateCallback) {
        // 通知外部监听者操作开始
        callback.onStart()

        // 生成随机耗时以模拟实际操作
        val time = (100..500).random().toLong()
        callback.onEnd(time)
    }

    // 事件监听器
    interface StateCallback {

        // 操作开始
        fun onStart()

        // 操作完成：通知消耗时长
        fun onEnd(time: Long)
    }
}
