package net.bi4vmr.study.mockk.base

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

/**
 * [UserManager]功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UserManagerTest {

    @Test
    fun test_GetUserNames() {
        // 模拟数据
        val mockDatas: Map<Long, String> = mapOf(1L to "来宾账户", 2L to "用户A", 3L to "用户B")

        // 创建DBHelper的Mock对象
        val mockDBHelper: DBHelper = mockk(relaxed = true)
        // 定义行为：如果 `queryUsers()` 方法被调用，则返回模拟数据。
        every { mockDBHelper.queryUsers() } returns mockDatas

        // 构造待测类的对象，并注入Mock对象作为依赖。
        val manager = UserManager()
        mockDBHelper.injectMock(manager, "mDBHelper")

        // 调用待测方法
        val users = manager.getUserNames()

        // 查看返回的内容
        users.forEachIndexed { index, s ->
            println("Index:[$index] Name:[$s]")
        }
        // 验证Mock对象的方法是否被调用
        verify { mockDBHelper.queryUsers() }
        // 验证待测方法的返回值是否与预期一致
        Assert.assertTrue(mockDatas.values.containsAll(users))
    }

    @Test
    fun test_GetUserNames2() {
        // 模拟数据
        val mockDatas: Map<Long, String> = mapOf(1L to "来宾账户", 2L to "用户A", 3L to "用户B")

        // 创建DBHelper的Mock对象（使用relaxed = true为没有明确定义行为的类添加默认行为）
        val mockDBHelper: DBHelper = mockk(relaxed = true)
        // 定义行为：如果 `queryUsers()` 方法被调用，则返回模拟数据。
        every { mockDBHelper.queryUsers() } returns mockDatas
        // 此处注释了定义 `DBHelper#saveLog()` 方法行为的语句。
        // every { mockDBHelper.saveLog(any()) } just runs

        // 构造待测类的对象，并注入Mock对象作为依赖。
        val manager = UserManager()
        mockDBHelper.injectMock(manager, "mDBHelper")

        // 调用待测方法
        val users = manager.getUserNames2()

        // 查看返回的内容
        users.forEachIndexed { index, s ->
            println("Index:[$index] Name:[$s]")
        }
        // 验证Mock对象的方法是否被调用
        verify { mockDBHelper.queryUsers() }
        // 验证待测方法的返回值是否与预期一致
        Assert.assertTrue(mockDatas.values.containsAll(users))
    }
}
