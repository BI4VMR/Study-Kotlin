package net.bi4vmr.study.advance

import io.mockk.every
import io.mockk.spyk
import org.junit.Test


/**
 * [MemoryInfo]功能测试。
 *
 * Spy使用案例。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class SpyTest {

    /**
     * 示例十五：Spy模式。
     *
     * 在本示例中，我们创建MemoryInfo的Spy对象，并为 `getFreeMemory()` 方法定义行为，模拟剩余内存较低的场景。
     */
    @Test
    fun test_Base() {
        // 创建Spy对象
        val spyMemoryInfo = spyk(MemoryInfo())

        println("初始状态...")
        println("内存总量：${spyMemoryInfo.getTotalMemory()}")
        println("空闲内存：${spyMemoryInfo.getFreeMemory()}")

        // 定义行为：模拟剩余内存为8KB的情况
        every { spyMemoryInfo.getFreeMemory() } returns 8 * 1024L

        println("定义行为后...")
        println("内存总量：${spyMemoryInfo.getTotalMemory()}")
        println("空闲内存：${spyMemoryInfo.getFreeMemory()}")
    }
}
