package net.bi4vmr.study.base

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import net.bi4vmr.study.injectMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * UserManager的测试类（使用注解）。
 *
 * 示例三：使用注解创建Mock对象。
 *
 * 在本示例中，我们以JUnit 4平台为例，使用MockK提供的注解创建Mock对象。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class AnnotationTest {

    /**
     * 创建一个DBHelper类的Mock对象。
     */
    @MockK
    lateinit var mockDBHelper1: DBHelper

    /**
     * 创建一个DBHelper类的Mock对象（宽松模式）。
     */
    @RelaxedMockK
    lateinit var mockDBHelper2: DBHelper

    /**
     * 创建一个DBHelper类的Mock对象（宽松模式）。
     */
    @MockK(relaxed = true)
    lateinit var mockDBHelper3: DBHelper

    /**
     * 创建一个DBHelper类的Mock对象（宽松模式，仅对无返回值的方法生效）。
     */
    @MockK(relaxUnitFun = true)
    lateinit var mockDBHelper4: DBHelper

    @Before
    fun setUp() {
        // 若要使用MockK注解，需要在执行其他操作前先初始化。
        MockKAnnotations.init(this)
    }

    @Test
    fun testGetUserNames() {
        // 模拟数据
        val mockDatas: Map<Long, String> = mapOf(1L to "来宾账户", 2L to "用户A", 3L to "用户B")

        // 定义行为：如果 `queryUsers()` 方法被调用，则返回模拟数据。
        every { mockDBHelper1.queryUsers() } returns mockDatas

        // 构造待测类的对象，并注入Mock对象作为依赖。
        val manager = UserManager()
        mockDBHelper1.injectMock(manager, "mDBHelper")

        // 调用待测方法
        val users = manager.getUserNames()

        // 查看返回的内容
        users.forEachIndexed { index, s ->
            println("Index:[$index] Name:[$s]")
        }
        // 验证Mock对象的方法是否被调用
        verify { mockDBHelper1.queryUsers() }
        // 验证待测方法的返回值是否与预期一致
        Assert.assertTrue(mockDatas.values.containsAll(users))
    }

    @Test
    fun testGetUserCount() {
        // 模拟数据
        val mockDatas: Map<Long, String> = mapOf(1L to "来宾账户", 2L to "用户A", 3L to "用户B")

        // 定义行为：如果 `queryUsers()` 方法被调用，则返回模拟数据。
        every { mockDBHelper1.queryUserCount() } returns mockDatas.size

        // 构造待测类的对象，并注入Mock对象作为依赖。
        val manager = UserManager()
        mockDBHelper1.injectMock(manager, "mDBHelper")

        // 调用待测方法
        val count = manager.getUserCount()

        // 查看返回的内容
        println("Count:[$count]")
        // 验证Mock对象的方法是否被调用
        verify { mockDBHelper1.queryUserCount() }
        // 验证待测方法的返回值是否与预期一致
        Assert.assertTrue(count == mockDatas.size)
    }
}
