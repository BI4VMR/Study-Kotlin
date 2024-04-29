package net.bi4vmr.study.classandobjects

/**
 * 测试代码 - 变量的作用域。
 *
 * @author BI4VMR
 */
fun main() {
    // 创建测试类的对象
    val test = TestScope()
    test.example01()
}

class TestScope {

    // 声明全局变量
    val x: Int = 1
    val y: String = "ABC"

    /*
     * 示例：全局变量。
     */
    fun example01() {
        // 访问全局变量
        println("x:[${x}]")
        println("y:[${y}]")
    }

    /*
     * 示例：局部变量。
     */
    fun function1() {
        // 声明局部变量"x"
        val x = 0
    }

    fun function2() {
        // 此方法无法访问"function1()"方法中的局部变量"x"，编译器会提示错误。
        // println(x)
    }
}
