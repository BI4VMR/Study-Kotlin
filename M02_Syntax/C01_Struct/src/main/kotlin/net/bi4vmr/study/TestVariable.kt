package net.bi4vmr.study

/**
 * 测试脚本 - 变量与常量。
 *
 * @author BI4VMR
 * @version 1.0
 */
fun main(args: Array<String>) {
    example02()
}

/*
 * 变量
 */
fun example01() {
    // 声明变量“姓名”
    val name: String = "张三"
    // 声明变量“年龄”
    val age: Int = 20

    println(name)
    println(age)
}

/*
 * 常量
 */
fun example02() {
    // 示例：常量"PI"
    val PI: Double = 3.141592653

    println(PI)
}
