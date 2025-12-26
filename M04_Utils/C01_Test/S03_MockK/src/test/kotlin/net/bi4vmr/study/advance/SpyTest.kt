package net.bi4vmr.study.advance

import io.mockk.every
import io.mockk.spyk
import net.bi4vmr.study.behavior_define.DBHelper
import org.junit.Test

/**
 * [LogManager]功能测试。
 *
 * Spy使用案例。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class SpyTest {


    /**
     * 示例六：偏函数模拟。
     *
     * 在本示例中，我们为Mock对象定义行为，每当指定方法被调用时，输出控制台消息。
     */
    @Test
    fun test_Define_OriginalCall() {
        val spyDBHelper = spyk(DBHelper())
        every { spyDBHelper.queryUserNames(any(), any()) } answers {
            val n = callOriginal()
            println("Origin Value: $n")
            ArrayList(n + "sss")
        }
    }
}
