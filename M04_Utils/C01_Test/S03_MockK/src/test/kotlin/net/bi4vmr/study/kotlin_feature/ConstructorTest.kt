package net.bi4vmr.study.kotlin_feature

import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.Test

/**
 * [Order]功能测试。
 *
 * 构造方法测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ConstructorTest {

    /**
     * 示例：模拟构造方法。
     *
     * 在本示例中，我们模拟类的构造方法。
     */
    @Test
    fun test_Base() {
        // 为Order类的构造方法启用Mock
        mockkConstructor(Order::class)

        // 定义行为：当Order类的任意构造方法被调用时，返回Mock对象并指定 `showInfo1()` 方法的行为。
        every { anyConstructed<Order>().showInfo1() } returns "[Mocked Order Info]"

        // 新建Order对象并检查方法的行为
        val order = Order("Apple")
        println("已Mock的 `showInfo1()` 方法：${order.showInfo1()}")
        println("未Mock的 `showInfo2()` 方法：${order.showInfo2()}")

        // 撤销指定类的构造方法Mock设置（可选）
        unmockkConstructor(Order::class)
    }

    /**
     * 示例：模拟特定条件的构造方法。
     *
     * 在本示例中，我们模拟输入参数与预定规则相符的构造方法。
     */
    @Test
    fun test_Matchers() {
        mockkConstructor(Order::class)

        // 定义行为：仅当构造Order对象的参数为"Apple"时，返回Mock对象并指定 `showInfo1()` 方法的行为。
        every {
            constructedWith<Order>(EqMatcher("Apple")).showInfo1()
        } returns "[Mocked Order Info]"

        // 新建Order对象并检查方法的行为
        println("使用Apple构造的实例：${Order("Apple").showInfo1()}")
        println("使用Banana构造的实例：${Order("Banana").showInfo1()}")

        // 撤销指定类的构造方法Mock设置（可选）
        unmockkConstructor(Order::class)
    }
}
