package net.bi4vmr.study.kotlin_feature

import io.mockk.every
import io.mockk.mockk
import net.bi4vmr.study.getField
import org.junit.Test

/**
 * TODO 添加简述。
 *
 * TODO 添加详情。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PrivateTest {

    @Test
    fun test() {
        val mockObject = mockk<Privates>()
        every { mockObject invoke "value" withArguments listOf() } returns 100L

        getField<Privates>(mockObject, "value").let {
            println("value = $it")
        }
    }
}
