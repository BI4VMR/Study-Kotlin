package net.bi4vmr.study.behavior_define

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * 定义行为。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DefineBehaviorTest {

    /**
     * 示例四：模拟固定返回值。
     *
     * 在本示例中，我们为Mock对象定义行为，每当指定方法被调用时，返回测试用例中指定的值。
     */
    @Test
    fun test_Define_Return_Value() {
        // 创建Mock对象
        val mockFile = mockk<File>()
        // 定义行为：当 `mockFile` 的 `getCanonicalPath()` 方法被访问时，返回 `/data/file1` 。
        every { mockFile.canonicalPath } returns "/data/file1"

        // 调用Mock对象的 `getCanonicalPath()` 方法并输出结果
        println("File Path:[${mockFile.canonicalPath}]")
        // 再次调用Mock对象的 `getCanonicalPath()` 方法并输出结果
        println("File Path:[${mockFile.canonicalPath}]\n")


        // 修改行为：当 `mockFile` 的 `getCanonicalPath()` 方法被访问时，返回 `/data/file2` 。
        every { mockFile.canonicalPath } returns "/data/file2"

        // 调用Mock对象的 `getCanonicalPath()` 方法并输出结果
        println("File Path:[${mockFile.canonicalPath}]")
    }

    /**
     * 示例五：模拟序列返回值。
     *
     * 在本示例中，我们为Mock对象定义行为，每当指定方法被调用时，依次返回不同的值。
     */
    @Test
    fun test_Define_Return_Sequence_Value() {
        // 创建Mock对象
        val mockFile = mockk<File>()
        // 设置每次调用时返回的值序列
        val mockResult = listOf(100L, 200L, 1024L)
        // 定义行为：当 `mockFile` 的 `length()` 方法被访问时，依次返回 `mockResult` 列表中的值。
        every { mockFile.length() } returnsMany mockResult

        // 多次访问Mock对象的属性并输出结果
        for (i in 1..5) {
            println("第 $i 次调用： Length:[${mockFile.length()}]")
        }
    }

    /**
     * 示例六：自定义行为。
     *
     * 在本示例中，我们为Mock对象定义行为，每当指定方法被调用时，输出控制台消息。
     */
    @Test
    fun test_Define_Answers() {
        // 创建Mock对象
        val mockFile = mockk<File>()
        // 定义行为：当 `mockFile` 的 `getCanonicalPath()` 方法被访问时，返回 `/data/file1` 。
        every { mockFile.canonicalPath } answers {
            // 输出消息
            println("$mockFile `canonicalPath()` was called.")

            // 此时 `answers {}` 块的最后一行将作为返回值
            "/data/file1"
        }

        // 调用Mock对象的 `getCanonicalPath()` 方法并输出结果
        println("File Path:[${mockFile.canonicalPath}]")
    }

    /**
     * 示例七：模拟异常。
     *
     * 在本示例中，我们为Mock对象定义行为，每当指定方法被调用时，抛出测试用例指定的异常。
     */
    @Test(expected = IOException::class)
    fun test_Define_Exception() {
        // 创建Mock对象
        val mockFile = mockk<File>()
        // 定义行为：当 `mockFile` 的 `getCanonicalPath()` 方法被访问时，抛出异常。
        every { mockFile.canonicalPath } throws IOException("This is a mock exception!")

        // 调用Mock对象的 `getCanonicalPath()` 方法并输出结果
        println("File Path:[${mockFile.canonicalPath}]")
    }

    /**
     * 示例八：参数匹配器。
     *
     * 在本示例中，我们使用参数匹配器定义Mock方法接收到不同参数时的行为。
     */
    @Test
    fun test_Parameter_Matchers() {
        val mockDBHelper = mockk<DBHelper>()
        // 定义不同条件下 `queryUserName()` 方法的返回值
        every { mockDBHelper.queryUserName(any<Int>()) } returns "MockUser"
        every { mockDBHelper.queryUserName(1) } returns "Alice"
        every { mockDBHelper.queryUserName(2) } returns "Bob"

        // 查看返回值
        println("QueryUserName of ID=1:[${mockDBHelper.queryUserName(1)}]")
        println("QueryUserName of ID=2:[${mockDBHelper.queryUserName(2)}]")
        println("QueryUserName of ID=3:[${mockDBHelper.queryUserName(3)}]")
    }

    /**
     * 示例九：匹配重载方法。
     *
     * 在本示例中，我们使用参数匹配器定义不同重载方法的行为。
     */
    @Test
    fun test_Parameter_Overloading() {
        val mockDBHelper = mockk<DBHelper>()

        // 匹配参数为Int类型的方法
        every { mockDBHelper.queryUserName(any<Int>()) } returns "MockUserA"
        // 匹配参数为Int类型的方法（等价写法）
        every { mockDBHelper.queryUserName(any(Int::class)) } returns "MockUserA"

        // 匹配参数为String类型的方法
        every { mockDBHelper.queryUserName(any<String>()) } returns "MockUserB"
        // 匹配参数为String类型的方法（等价写法）
        every { mockDBHelper.queryUserName(any(String::class)) } returns "MockUserB"

        // 查看返回值
        println("QueryUserName by ID:[${mockDBHelper.queryUserName(1)}]")
        println("QueryUserName by CardID:[${mockDBHelper.queryUserName("1999")}]")
    }

    /**
     * 示例十：具体值与 `eq()` 匹配器。
     *
     * 在本示例中，我们演示参数具体值与 `eq()` 匹配器的区别。
     */
    @Test
    fun test_Parameter_ValueAndEQ() {
        val mockDBHelper = mockk<DBHelper>()

        // 错误用法：字面量与匹配器混用
        every { mockDBHelper.queryUserNames(20, any()) } returns listOf()

        // 正确用法：全部使用匹配器
        every { mockDBHelper.queryUserNames(eq(20), any()) } returns listOf()
        // 正确用法：全部使用字面量
        every { mockDBHelper.queryUserNames(20, false) } returns listOf()
    }
}
