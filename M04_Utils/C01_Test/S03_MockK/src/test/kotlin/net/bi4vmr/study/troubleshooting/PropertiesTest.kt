package net.bi4vmr.study.troubleshooting

import io.mockk.every
import io.mockk.mockk
import org.junit.Test

/**
 * 案例一：TODO 添加详情。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PropertiesTest {

    @Test
    fun test() {
        // 尝试为 `getState()` 方法设置模拟返回值。
        val mockObj = mockk<Properties>()

        // 此处将会执行失败，因为 `state` 属性生成了与 `getState()` 方法同名的Getter方法，JVM允许重载，但MockK无法指定要Mock的具体方法。
        every { mockObj.getState() } returns 1000
    }
}
