package net.bi4vmr.study.abstractclass

/**
 * 测试代码：抽象类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}

/**
 * 示例：使用抽象类及其实现类。
 *
 * 在本示例中，创建抽象类的子类实例，并调用抽象类和子类中的方法。
 */
fun example01() {
    // 创建抽象类子类的实例
    val dog = Dog()
    // 调用Animal类中的普通方法
    dog.speak()
    // 调用Dog类实现的抽象方法
    dog.eat()
}
