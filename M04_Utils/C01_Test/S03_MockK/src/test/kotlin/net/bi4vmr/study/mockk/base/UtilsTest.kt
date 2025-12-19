package net.bi4vmr.study.mockk.base

import io.mockk.*
import org.junit.Test

/**
 * [Utils]功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UtilsTest {

    /**
     * 示例X：模拟Object中的普通方法。
     *
     * 在本示例中，我们模拟Object中的非静态方法。
     */
    @Test
    fun test_Mock_Object() {
        // 为Utils中的普通方法启用Mock
        mockkObject(Utils)
        // 定义行为
        every { Utils.getCurrentTime() } returns 1234567890L

        // 调用Mock方法
        println("Utils#getCurrentTime:[${Utils.getCurrentTime()}]")

        // 撤销Mock（可选）
        unmockkObject(Utils)
    }

    /**
     * 示例X：模拟Object中的静态方法。
     *
     * 在本示例中，我们模拟Object中的JVM静态方法。
     */
    @Test
    fun test_Mock_Static() {
        // 为Utils中的静态方法启用Mock
        mockkStatic(Utils::class)

        // 定义行为
        every { Utils.getURL() } returns "http://test.com/"

        // 调用Mock方法
        println("Utils#getURL:[${Utils.getURL()}]")

        // 撤销Mock（可选）
        unmockkStatic(Utils::class)
    }

    /**
     * 示例X：模拟伴生对象中的方法。
     *
     * 在本示例中，我们模拟类的伴生对象中的方法。
     */
    @Test
    fun test_Mock_Companion_Object() {
        // 为Utils2伴生对象中的方法启用Mock
        mockkObject(Utils2)

        // 使用该语句也能启用伴生对象的Mock
        // mockkObject(Utils2.Companion)

        every { Utils2.method() } returns "Test method."
        every { Utils2.methodStatic() } returns "Test static method."

        println("Utils2#method:[${Utils2.method()}]")
        println("Utils2#methodStatic:[${Utils2.methodStatic()}]")
    }
}
