package net.bi4vmr.study.kotlin_feature

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * [Properties]功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PropertiesTest {

    /**
     * 示例十九：模拟与验证属性的Get调用。
     *
     * 在本示例中，我们模拟与验证Kotlin属性的Get调用。
     */
    @Test
    fun test_Get() {
        val mock = mockk<Properties>()

        // 定义行为：如果调用者访问 `name` 属性，则返回 "Mock User"。
        every { mock.name } returns "Mock User"
        // 对于读取属性的调用，也可以使用 `getProperty()` 进行定义，与前一行等价。
        every { mock getProperty ("name") } returns "Mock User"

        println("访问 `name` 属性：${mock.name}")

        // 验证属性是否已被访问
        verify { mock.name }
        // 对于读取属性的调用，也可以使用 `getProperty()` 进行验证，与前一行等价。
        verify { mock getProperty ("name") }
    }

    /**
     * 示例二十：模拟与验证属性的Set调用。
     *
     * 在本示例中，我们模拟与验证Kotlin属性的Set调用。
     */
    @Test
    fun test_Set() {
        val mock = mockk<Properties>()

        // 暂存调用者设置的值
        var mockAge = 0
        // 对于设置属性的调用，需要使用 `setProperty()` 进行定义，并且可以设置匹配器。
        every { mock setProperty ("age") value less(0) } answers {
            // 可以通过 `firstArg<T>()` 获取属性值
            val v = firstArg<Int>()
            println("年龄不能为负数($v)，最低为 `0` 。")
            mockAge = 0
        }
        every { mock.age } returns mockAge

        // 设置属性
        println("试图设置 `age` 属性为 -1 。")
        mock.age = -1
        println("访问 `age` 属性：${mock.age}")

        // 验证属性是否已被设置
        verify { mock setProperty ("age") value less(0) }
    }
}
