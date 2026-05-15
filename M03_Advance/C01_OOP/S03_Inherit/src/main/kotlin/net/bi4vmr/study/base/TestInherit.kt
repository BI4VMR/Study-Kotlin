package net.bi4vmr.study.base

/**
 * 测试代码：继承。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}

/**
 * 示例一：继承的基本应用。
 *
 * 在本示例中，声明父类变量分别指向父类和子类，调用各自的方法。
 */
fun example01() {
    // 声明Father变量"t1"，指向父类的引用。
    val t1: Father = Father()
    // 声明Father变量"t2"，指向子类的引用。
    val t2: Father = Child()

    // 分别执行两个变量的"show()"方法
    t1.show()
    t2.show()
}

/**
 * 示例二：使用super调用父类成员。
 *
 * 在本示例中，子类方法在调用父类方法的基础上增加自己的逻辑。
 */
fun example02() {
    // 定义一个扩展子类
    val child = object : Father() {
        override fun show() {
            // 调用父类的方法
            super.show()
            println("This is extended in Child.")
        }
    }
    child.show()
}
