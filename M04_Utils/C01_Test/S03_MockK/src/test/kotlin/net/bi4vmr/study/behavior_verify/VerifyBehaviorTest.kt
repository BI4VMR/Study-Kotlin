package net.bi4vmr.study.behavior_verify

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 验证行为。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class VerifyBehaviorTest {

    /**
     * 示例十一：基本的行为验证。
     *
     * 在本示例中，我们使用 `verify {}` 方法验证被测接口是否正确地调用了依赖组件。
     */
    @Test
    fun test_Base() {
        // 创建Logger的Mock对象
        val mockLogger: Logger = mockk(relaxed = true)

        // 创建待测类的实例并注入Mock对象
        val manager = LogManager()
        manager.logger = mockLogger
        // 执行业务方法
        val text: List<String> = listOf("LineA", "LineB")
        manager.printInfo(text)

        // 验证行为：Logger的记录方法应当被调用2次
        verify(exactly = 2) {
            mockLogger.log(eq(Level.INFO), any<String>())
        }


        // 再次执行业务方法
        val text2: List<String> = listOf("Line1", "Line2", "Line3")
        manager.printInfo(text2)

        // 验证行为：Logger的记录方法应当被调用2次 + 3次
        verify(exactly = 2 + text2.size) {
            mockLogger.log(eq(Level.INFO), any<String>())
        }
    }

    /**
     * 示例十二：验证一组方法的调用顺序。
     *
     * 在本示例中，我们使用 `verifySequence {}` 方法验证被测接口是否正确地调用了依赖组件。
     */
    @Test
    fun test_Sequence() {
        // 创建监听器的Mock对象
        val mockListener: LogManager.StateCallback = mockk(relaxed = true)

        // 执行业务方法并传入监听器
        LogManager().saveLog(mockListener)

        // 验证行为：日志导出后，监听器中的起始和结束方法都应当被调用，且符合先开始后结束的顺序。
        verifySequence {
            mockListener.onStart()
            mockListener.onEnd(any())
        }
    }
}
