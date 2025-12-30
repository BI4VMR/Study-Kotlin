package net.bi4vmr.study.advance

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import net.bi4vmr.study.getMethod
import org.junit.Test

/**
 * 私有方法测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PrivatesTest {

    /**
     * 示例二十六：模拟与验证私有方法。
     *
     * 在本示例中，我们为类的私有方法设置Mock行为，并进行调用与验证。
     */
    @Test
    fun test_Base() {
        // 创建Spy对象并开启私有方法调用记录
        val mock = spyk<Privates>(recordPrivateCalls = true)

        // 定义行为：当私有方法 `foo()` 被调用时，返回 `Mock!` 。
        every { mock invoke "foo" withArguments listOf(any<String>()) } returns "Mock!"

        // 简化写法
        every { mock["foo"].invoke(any<String>()) } returns "Mock!"

        // 通过反射调用私有方法
        mock.getMethod("foo", String::class.java)?.let {
            val result = it.invoke(mock, "Hello!")
            println("私有方法的返回值：$result")
        }

        // 验证私有方法是否被调用
        verify { mock["foo"].invoke("Hello!") }
    }
}
