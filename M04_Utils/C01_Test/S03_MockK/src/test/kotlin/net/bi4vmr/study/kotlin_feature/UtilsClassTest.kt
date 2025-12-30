package net.bi4vmr.study.kotlin_feature

import io.mockk.every
import io.mockk.mockkObject
import org.junit.Test

/**
 * [UtilsClass]功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UtilsClassTest {

    /**
     * 示例十六：模拟伴生对象中的方法。
     *
     * 在本示例中，我们模拟类的伴生对象中的方法。
     */
    @Test
    fun test_Mock_Companion_Object() {
        // 为UtilsClass伴生对象中的方法启用Mock
        mockkObject(UtilsClass)

        // 使用该语句也能启用伴生对象的Mock
        // mockkObject(UtilsClass.Companion)

        every { UtilsClass.method() } returns "Test method."
        every { UtilsClass.methodStatic() } returns "Test static method."

        println("UtilsClass#method:[${UtilsClass.method()}]")
        println("UtilsClass#methodStatic:[${UtilsClass.methodStatic()}]")
    }
}
