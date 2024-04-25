package net.bi4vmr.study

import java.math.BigDecimal

/**
 * Name        : TestComment
 *
 * Description : 测试类 - Java文档注释。
 *
 * @author BI4VMR
 * @version 1.0
 */
class TestComment {

    /**
     * Name        : 计算两数之和
     *
     * Description : 功能类似于[BigDecimal.add]。
     *
     * @param a 运算数1。
     * @param b 运算数2。
     * @return 两个运算数的和。
     * @since 1.0
     * @throws IllegalArgumentException 除数为"0"。
     * @see java.math.BigDecimal.add(BigDecimal)
     */
    fun divide(a: Int, b: Int): Int {
        if (b == 0) {
            throw IllegalArgumentException("除数不能为0！")
        }

        // 返回两个参数之商
        return a / b
    }
}
