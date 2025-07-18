package net.bi4vmr.study

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 测试代码：协程测试工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@FixMethodOrder(MethodSorters.JVM)
class TestTools {

    @Test
    fun test_RunTest() = runTest {
        task()
    }

    @Test
    fun test_RunTest2() = runTest {
        CoroutineScope(Dispatchers.Default).launch {
            task()
        }
        testScheduler.advanceUntilIdle()
    }

    suspend fun task() {
        println("Task start. Time:[${getTime()}]")
        delay(5_000L)
        println("Task end. Time:[${getTime()}]")
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
