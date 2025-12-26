package net.bi4vmr.study.kotlin_feature

import io.mockk.every
import io.mockk.mockk
import org.junit.Test

/**
 * TODO 添加简述。
 *
 * TODO 添加详情。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PropertiesTest {

    @Test
    fun aaa(){
        val mock = mockk<Properties>()
        // every { mock.value1 } returns 100
        every { mock getProperty("value111")  } returns 100
        every { mock setProperty ("value1") value more(1)  } answers {
            val v = firstArg<Int>()
            println("set value1 to $v")
        }

        mock.value1.let { println(it) }
        // mock.value111.let { println(it) }
    }
}
