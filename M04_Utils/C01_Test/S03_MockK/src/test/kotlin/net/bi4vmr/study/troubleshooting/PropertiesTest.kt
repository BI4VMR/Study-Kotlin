package net.bi4vmr.study.troubleshooting

import io.mockk.every
import io.mockk.mockk
import org.junit.Test

/**
 * 案例一：模拟方法时，出现类型不匹配错误。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PropertiesTest {

    @Test
    fun test() {
        // 尝试为 `getState()` 方法设置模拟返回值。
        val mockObj = mockk<Properties>()

        /*
         * 执行失败，因为 `state` 属性生成了与 `getState()` 方法同名的Getter方法，JVM允许同名不同返回值的方法在字节码中共存，但MockK
         * 无法指定目标方法，导致Mock失败。
         */
        every { mockObj.getState() } returns 1000
    }
}
