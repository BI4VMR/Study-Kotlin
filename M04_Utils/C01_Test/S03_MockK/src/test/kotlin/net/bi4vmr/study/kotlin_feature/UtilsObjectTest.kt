package net.bi4vmr.study.kotlin_feature

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.Test

/**
 * [UtilsObject]功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UtilsObjectTest {

    /**
     * 示例十四：模拟Object中的普通方法。
     *
     * 在本示例中，我们模拟Object中的非静态方法。
     */
    @Test
    fun test_Mock_Object() {
        // 为UtilsObject中的普通方法启用Mock
        mockkObject(UtilsObject)
        // 定义行为
        every { UtilsObject.getCurrentTime() } returns 1234567890L

        // 调用Mock方法
        println("UtilsObject#getCurrentTime:[${UtilsObject.getCurrentTime()}]")

        // 撤销指定Object的Mock设置（可选）
        unmockkObject(UtilsObject)
        // 撤销所有Object、Static、构造方法的Mock设置（可选）
        unmockkAll()
    }

    /**
     * 示例十五：模拟Object中的静态方法。
     *
     * 在本示例中，我们模拟Object中的JVM静态方法。
     */
    @Test
    fun test_Mock_Static() {
        // 为UtilsObject中的静态方法启用Mock
        mockkStatic(UtilsObject::class)

        // 定义行为
        every { UtilsObject.getURL() } returns "http://test.com/"

        // 调用Mock方法
        println("UtilsObject#getURL:[${UtilsObject.getURL()}]")

        // 撤销指定Object的Mock设置（可选）
        unmockkStatic(UtilsObject::class)
    }
}
