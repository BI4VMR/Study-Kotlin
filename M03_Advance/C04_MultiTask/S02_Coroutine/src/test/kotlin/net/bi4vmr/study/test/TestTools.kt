package net.bi4vmr.study.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.bi4vmr.study.scope.UserManagerV2
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
     * 示例一：使用 `runTest()` 方法测试挂起函数。
     */
    @Test
    fun test_Base() = runTest {
        task()
    }

    suspend fun task() {
        println("Task start. Time:[${getTime()}]")
        delay(60_000L)
        println("Task end. Time:[${getTime()}]")
    }

    /**
     * 示例一：使用TestScheduler控制协程。
     */
    @Test
    fun test_TestScheduler() = runTest {
        // 使用TestScope提供的Scheduler，创建测试调度器。
        val dispatcher = StandardTestDispatcher(testScheduler)

        // 将测试调度器注入到待测组件，并执行业务方法。
        UserManagerV2(dispatcher).updateCurrentUID()

        // 测试协程等待业务协程执行完成
        advanceUntilIdle()

        println("Test task end.")
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
