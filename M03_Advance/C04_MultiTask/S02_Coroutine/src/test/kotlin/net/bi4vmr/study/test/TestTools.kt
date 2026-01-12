package net.bi4vmr.study.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 示例代码：测试工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestTools {

    /**
     * 示例一：测试挂起函数。
     *
     * 在本示例中，我们通过 `runTest()` 方法创建测试协程作用域，并在该作用域内调用挂起函数。
     */
    @Test
    fun test_Base() = runTest {
        task()
        println("Test end.")
    }

    suspend fun task() {
        println("Task start. Time:[${getTime()}]")
        delay(60_000L)
        println("Task end. Time:[${getTime()}]")
    }

    /**
     * 示例二：通过TestScheduler控制协程进度。
     *
     * 在本示例中，我们使用TestScheduler控制被测协程的执行进度，确保测试协程等待被测协程执行完毕。
     */
    @Test
    fun test_TestScheduler() = runTest {
        // 使用TestScope提供的Scheduler，创建测试调度器。
        val dispatcher = StandardTestDispatcher(testScheduler)

        // 将测试调度器注入到待测组件，并执行业务方法。
        val manager = DataManager(dispatcher)
        println("Free space is ${manager.freeSpace} before clean.")
        manager.clearCache()

        // 测试协程等待业务协程执行完成
        advanceUntilIdle()

        // 业务协程执行完毕后再断言结果
        println("Free space is ${manager.freeSpace} after clean.")
        Assert.assertTrue(manager.freeSpace > 10)
    }

    /**
     * 获取当前时间。
     *
     * @return 时间字符串（HH:mm:ss.SSS）。
     */
    private fun getTime(): String {
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        return LocalTime.now().format(dateFormatter)
    }
}
