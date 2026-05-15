package net.bi4vmr.study.base

/**
 * 测试代码：接口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}

/**
 * 示例一：接口的基本应用。
 *
 * 在本示例中，定义集合将元素类型限定为Animal接口，
 * 创建接口实现类的实例并调用接口方法。
 */
fun example01() {
    // 定义集合，元素需要满足"动物"接口
    val animals = mutableListOf<Animal>()

    // 创建Animal接口实现类的实例
    val dog = Dog()
    val cat = Cat()

    // 将实例添加至集合
    animals.add(dog)
    animals.add(cat)

    // 调用接口中定义的方法
    for (animal in animals) {
        animal.eat()
    }
}
