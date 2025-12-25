package net.bi4vmr.study.advance

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.logging.Level

/**
 * [LogManager]功能测试。
 *
 * 参数捕获器使用案例。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class CaptorTest {

    /**
     * 示例：捕获回调接口并模拟事件。
     *
     * 在本示例中，我们捕获被测对象向依赖组件注册的监听器实例，并模拟事件触发。
     */
    @Test
    fun test_Base() {
        // 定义行为：当LogConfigTool的 `addConfigListener()` 方法被调用时，捕获调用者传入的实例。
        val slot = slot<LogConfigTool.ConfigListener>()
        mockkObject(LogConfigTool)
        every { LogConfigTool.addConfigListener(capture(slot)) } just runs

        // 创建被测类的实例
        val manager = LogManager()
        println("初始的日志级别：${manager.minLevel}")

        // 调用捕获到的监听器方法，模拟事件回调。
        slot.captured.onLevelChange(Level.WARNING)

        println("事件触发后的日志级别：${manager.minLevel}")
        // 验证事件触发是否确实改变了被测对象的属性
        assertEquals(Level.WARNING, manager.minLevel)
    }

    /**
     * 示例：捕获多次调用的参数。
     *
     * 在本示例中，我们捕获被测对象向依赖组件注册的监听器实例，并计算平均耗时。
     */
    @Test
    fun test_Multiple() {
        // 创建列表以接收多次调用参数
        val slots = mutableListOf<Long>()
        // 创建监听器的Mock对象
        val mockListener: LogManager.StateCallback = mockk(relaxed = true)
        // 定义行为：当监听器的 `onEnd()` 方法被调用时，捕获传入的参数。
        every { mockListener.onEnd(capture(slots)) } just runs

        // 创建被测类的实例
        val manager = LogManager()
        // 调用5次保存日志的方法
        repeat(5) {
            manager.saveLog(mockListener)
        }

        // 查看捕获到的参数
        slots.forEachIndexed { i, v ->
            println("第${i + 1}次调用，耗时：${v} ms。")
        }
        // 计算平均耗时
        println("平均耗时：${slots.average()} ms。")
    }
}
