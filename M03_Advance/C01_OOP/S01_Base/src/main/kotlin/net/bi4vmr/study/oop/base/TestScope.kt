package net.bi4vmr.study.oop.base

/**
 * 测试代码：变量的作用域。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {

    /**
     * 示例二：全局变量与默认值。
     * <p>
     * 在本示例中，我们定义一个测试类并声明若干全局变量，并在控制台上输出它们的值。
     */
    // 创建测试类的对象
    val test = TestScope()
    test.example02()
}


class TestScope {

    // 声明全局变量
    val x: Int = 1
    val y: String = "ABC"

    /*
     * 示例：全局变量。
     */
    fun example02() {
        // 访问全局变量
        println("x:[${x}]")
        println("y:[${y}]")
    }

    /**
     * 示例三：局部变量。
     * <p>
     * 在本示例中，我们在一个方法内定义变量，并尝试在另一个方法中访问它。
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
