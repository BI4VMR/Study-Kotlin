package net.bi4vmr.study.abstractclass

/**
 * 抽象类示例：动物。
 *
 * 抽象类使用"abstract"关键字声明，不能被实例化。
 * 与接口不同，抽象类可以有构造函数和状态（属性值）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
abstract class Animal {

    // 定义属性
    private val name = "Animal"

    // 定义抽象方法（子类必须实现）
    abstract fun eat()

    // 定义普通方法（子类直接继承）
    fun speak() {
        println("这是一个$name")
    }
}
