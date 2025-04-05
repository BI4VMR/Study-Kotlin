package net.bi4vmr.study.mockk.base

import io.mockk.*
import org.junit.Test

/**
 * Utils的测试类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UtilsTest {

    @Test
    fun testMockObject() {
        // Mock Utils中的普通方法
        mockkObject(Utils)
        // 定义行为
        every { Utils.getCurrentTime() } returns 1234567890L
        // 调用Mock方法
        println("Utils#getCurrentTime:[${Utils.getCurrentTime()}]")

        // 撤销Mock（可选）
        unmockkObject(Utils)
    }

    @Test
    fun testMockStatic() {
        // Mock Utils中的静态方法
        mockkStatic(Utils::class)
        // 定义行为
        every { Utils.getURL() } returns "http://example.com/"
        // 调用Mock方法
        println("Utils#getURL:[${Utils.getURL()}]")

        // 撤销Mock（可选）
        unmockkStatic(Utils::class)
    }
}
