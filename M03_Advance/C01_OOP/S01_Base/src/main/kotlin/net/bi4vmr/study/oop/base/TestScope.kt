package net.bi4vmr.study.oop.base

/**
 * 测试代码：变量的作用域。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example02()
}


/**
 * 示例二：全局变量的基本应用。
 *
 * 在本示例中，我们定义测试类并声明若干全局变量，然后在方法中访问它们。
 */
fun example02() {
    // 创建测试类的对象
    val instance = TestScope("ABC")
    // 访问全局变量
    instance.useGlobalVariables()
}

class TestScope {

    // 声明全局变量并指定初始值
    val x: Int = 1

    // 声明全局变量但由构造方法指定初始值
    val y: String

    constructor(value: String) {
        // 使用构造方法的参数为全局变量设置初始值。
        y = value
    }

    // 访问全局变量
    fun useGlobalVariables() {
        println("x:[$x]")
        println("y:[$y]")
    }

    // 局部变量测试方法一
    fun function1() {
        // 声明局部变量 `temp`
        val temp = 0
    }

    // 局部变量测试方法二
    fun function2() {
        // 此处无法访问 `function1()` 方法中的局部变量 `temp` ，编译器会提示错误。
        // println(temp)
    }
}


/**
 * 示例三：局部变量的基本应用。
 *
 * 在本示例中，我们定义一个方法并声明局部变量，然后在另一个方法中访问它。
 */
fun example03() {
    // 创建测试类的对象
    val instance = TestScope("ABC")
    // 访问局部变量
    instance.function1()
    instance.function2()
}
