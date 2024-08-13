package net.bi4vmr.study.struct

import java.math.BigDecimal

/**
 * 测试代码 - Kotlin文档注释。
 *
 * @since 1.0.0
 * @author bi4vmr@outlook.com
 */
class TestComment {

    /**
     * 计算两数之和。
     *
     * 功能类似于[BigDecimal.add]。
     *
     * @param[a] 运算数1。
     * @param[b] 运算数2。
     * @return 两个运算数的和。
     * @since 1.0
     * @throws[IllegalArgumentException] 除数为"0"。
     * @see BigDecimal.add
     */
    fun divide(a: Int, b: Int): Int {
        if (b == 0) {
            throw IllegalArgumentException("除数不能为0！")
        }

        // 返回两个参数之商
        return a / b
    }
}
