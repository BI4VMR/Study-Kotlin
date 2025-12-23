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

        verify(exactly = 2) {
            mockLogger.log(eq(Level.INFO), any<String>())
        }
    }

    @Test
    fun test() {
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
