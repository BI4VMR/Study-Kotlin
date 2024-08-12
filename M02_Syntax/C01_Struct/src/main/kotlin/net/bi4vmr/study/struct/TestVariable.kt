package net.bi4vmr.study.struct

/**
 * 测试代码 - 变量。
 *
 * @author bi4vmr@outlook.com
 */
fun main(args: Array<String>) {
    example02()
}

/**
 * 示例：变量的基本应用。
 * <p>
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
 * <p>
 * 在本示例中，我们定义一些常量，并访问它们。
 */
fun example02() {
    // 示例：常量"PI"
    val PI: Double = 3.141592653

    // 访问常量：将常量的值输出到控制台
    println("圆周率：${PI}")
}
