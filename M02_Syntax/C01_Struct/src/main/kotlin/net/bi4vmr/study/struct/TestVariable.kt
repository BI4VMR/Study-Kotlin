package net.bi4vmr.study.struct

/**
 * 测试代码 - 变量。
 *
 * @since 1.0.0
 * @author bi4vmr@outlook.com
 */
fun main(args: Array<String>) {
    example03()
}

/**
 * 示例：变量的基本应用。
 *
 * 在本示例中，我们定义一些变量，并访问它们。
 */
fun example01() {
    // 声明变量“姓名”
    val name: String = "张三"
    // 声明变量“年龄”
    val age: Int = 20

    // 访问变量：将变量的值输出到控制台
    println("姓名：$name")
    println("年龄：$age")
}

/**
 * 示例：常量的基本应用。
 *
 * 在本示例中，我们定义一些常量，并访问它们。
 */
fun example02() {
    // 示例：常量"PI"
    val PI: Double = 3.141592653

    // 访问常量：将常量的值输出到控制台
    println("圆周率：${PI}")
}

/**
 * 示例：变量的作用域。
 *
 * 在本示例中，我们定义一些变量，并在不同的作用域中访问它们。
 */
fun example03() {
    val i: Int = 1

    // 定义一个“块”
    run {
        val x: Int = 5
        // 块的内部可以访问自己的变量与父级块的变量
        println("i:$i; x:$x")
    }
    // 块的外部不能访问块内部的变量
    println("i:$i; x:can not be accessed!")
}
