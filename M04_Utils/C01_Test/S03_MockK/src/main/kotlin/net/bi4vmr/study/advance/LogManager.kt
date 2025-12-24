package net.bi4vmr.study.advance

import java.util.logging.Level
import java.util.logging.Logger

/**
 * 日志工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class LogManager {

    // 外部依赖
    var logger: Logger = Logger.getAnonymousLogger()

    var minLevel: Level = Level.INFO
        private set

    // 业务方法：将一组消息分行输出
    fun printInfo(messages: List<String>) {
        messages.forEach { message ->
            logger.log(Level.INFO, message)
        }
    }

    // 业务方法：导出日志
    fun saveLog(callback: StateCallback) {
        // 通知外部监听者操作开始
        callback.onStart()

        // 模拟耗时操作
        Thread.sleep(200L)

        // 通知外部监听者操作结束
        callback.onEnd(200L)
    }

    // 事件监听器
    interface StateCallback {

        // 操作开始
        fun onStart()

        // 操作完成：通知消耗时长
        fun onEnd(time: Long)
    }
}
