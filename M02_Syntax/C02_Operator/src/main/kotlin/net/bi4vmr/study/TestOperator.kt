package net.bi4vmr.study

import kotlin.math.sqrt

fun main() = example07()


/**
 * 示例一：赋值运算符的基本应用。
 *
 * 在本示例中，我们使用赋值运算符操作变量。
 */
fun example01() {
    var a = 5
    var b = a

    // 使用普通表达式
    a = a + 2
    println("a: $a")

    // 使用加等于符号
    b += 2
    println("b: $b")
}


/**
 * 示例二：基本的四则运算。
 *
 * 在本示例中，我们使用算术运算符进行基本的四则运算。
 */
fun example02() {
    val a = 5 + 12
    val b = 25 - 10
    val c = 3 * 8
    val d = 24 / 7
    val e = 24 % 7
    val f = 24.0 / 7

    println("5 + 12 = $a")
    println("25 - 10 = $b")
    println("3 * 8 = $c")
    println("24 / 7 = $d")
    println("24 % 7 = $e")
    println("24.0 % 7 = $f")
}


/**
 * 示例三：结果为无穷大的表达式。
 *
 * 在本示例中，我们使用某个数除以浮点数的 `0` ，并观察计算结果。
 */
fun example03() {
    // 非0正数除以0，结果为正无穷。
    val a = 100 / 0.0
    println("100除以0.0：$a")

    // 非0负数除以0，结果为负无穷。
    val b = -100 / 0.0
    println("-100除以0.0：$b")
}


/**
 * 示例四：理解无穷大的含义。
 *
 * 在本示例中，我们对无穷大数值进行判等操作与类型转换。
 */
fun example04() {
    // 比较两个正无穷值
    val x = 1 / 0.0F
    val y = 2 / 0.0
    println(x.toDouble() == y)
    println(x == Float.POSITIVE_INFINITY)

    // 将正无穷强制转换为整数
    println(x.toInt())
    println(x.toLong())
}


/**
 * 示例五：理解 `NaN` 的含义。
 *
 * 在本示例中，我们列举一些结果为 `NaN` 的表达式，并对 `NaN` 数值进行判等操作。
 */
fun example05() {
    // 浮点型0除以0结果为NaN
    val a = 0.0 / 0.0
    println("a = 0.0 / 0.0 = $a")

    // 负数的平方根结果为NaN
    val b = sqrt(-2.0)
    println("b = sqrt(-2) = $b")

    // 比较两个NaN值是否相等
    println("a == a ? ${a == a}")
    println("a == b ? ${a == b}")
    println("a is NaN ? ${a.isNaN()}")
}


/**
 * 示例六：自增运算符的基本应用。
 *
 * 在本示例中，我们使用自增运算符操作变量。
 */
fun example06() {
    var a = 5
    var b = 5

    println("自增符号在前：${++a}")
    println("自增符号在后：${b++}")
    println("a = $a ;b = $b")
}


/**
 * 示例七：比较运算符的基本应用。
 *
 * 在本示例中，我们使用比较运算符操作变量。
 */
fun example07() {
    val a = 5
    val b = 1

    println("a>b: ${a > b}")
    println("a<b: ${a < b}")

    val s1 = "Test"
    val s2 = String("Test".toCharArray())
    println("s1内容是否与s2相同：${s1 == s2}")
}


/**
 * 示例八：普通运算与短路运算的区别。
 *
 * 在本示例中，我们比较普通运算与短路运算的区别。
 */
fun example08() {
    var a = 5

    // "&&"是短路运算，左侧为false时右侧不会执行
    val b1 = false && (a++ == 6)
    println("a = $a")

    // Kotlin无非短路逻辑运算符"&"，此处以"and"扩展函数模拟（参数会被求值）
    val b2 = false.and(a++ == 6)
    println("a = $a")
}


/**
 * 示例九：位运算符的基本应用。
 *
 * 在本示例中，我们使用位运算符进行位运算。
 */
fun example09() {
    val a = 0x25        // 25H  -> 0010 0101B
    val b = 0xCE        // CEH  -> 1100 1110B
    val m = a and b     // 4D   -> 0000 0100B
    val n = a or b      // 239D -> 1110 1111B
    val o = a.inv()     // -38D -> 1101 1010B
    val p = a xor b     // 235D -> 1110 1011B
    val x = a shl 2     // 148D -> 1001 0100B
    val y = a shr 2     // 9D   -> 0000 1001B

    println("a    = $a")
    println("b    = $b")
    println("a&b  = $m")
    println("a|b  = $n")
    println("~a   = $o")
    println("a^b  = $p")
    println("a<<2 = $x")
    println("a>>2 = $y")
}


/**
 * 示例十：条件运算符的基本应用。
 *
 * 在本示例中，我们定义两个变量 `a` 与 `b` ，如果 `a` 大于 `b` 则将 `a` 的 `100` 倍赋值给变量 `c` ；反之将 `b` 的 `100` 倍赋值给 `c` 。
 */
fun example10_1() {
    val a = 1
    val b = 2

    val c = if (a >= b) (a * 100) else (b * 100)
    println("c = $c")
}


/**
 * 示例十：条件运算符的等价写法。
 */
fun example10_2() {
    val a = 1
    val b = 2
    val c: Int

    if (a >= b) {
        c = a * 100
    } else {
        c = b * 100
    }
    println("c = $c")
}
