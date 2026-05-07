package net.bi4vmr.study.lambda

import java.util.Collections

/**
 * 示例代码：Lambda表达式。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例一：表示二维坐标。
 *
 * 在本示例中，我们定义一个类表示二维坐标，允许调用者传入整数、小数等多种形式的原始数据。
 */
fun example01() {
    val list = listOf(1, 2, 3, 4, 5)
    // list.filter(object : (Int) -> Boolean {
    //     override fun invoke(p1: Int): Boolean {
    //         return p1 % 2 == 0
    //     }
    // }).forEach { println(it) }

    Collections.sort(list, object : Comparator<Int> {
        override fun compare(o1: Int, o2: Int): Int {
            // 倒序排列
            return o2 - o1
        }
    })

    list.forEach { println(it) }

    Collections.sort(list, Comparator<Int> { o1, o2 -> o2 - o1 })

    // val aa: (x: Int, y: Boolean) -> Unit = { x, y -> println("x = $x, y = $y") }
    // aa.invoke(1, false)
}
