package net.bi4vmr.study.base

/**
 * 测试代码：多态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example02()
}

/**
 * 示例一：多态的基本应用。
 *
 * 在本示例中，声明Father类型的变量分别指向父类和子类的实例，
 * 调用同一方法时各自表现出不同的行为，这就是"多态"。
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
 * 示例二：类型判断与智能转换。
 *
 * Kotlin使用"is"关键字判断类型（对应Java的instanceof），
 * 并且支持"智能转型"：经过is判断后，变量会被自动转换为对应类型，
 * 无需手动添加强制类型转换语句。
 */
fun example02() {
    // 定义Animal接口和实现类
    open class Animal
    class Dog : Animal() {
        fun bark() = println("汪汪！")
    }
    class Cat : Animal() {
        fun meow() = println("喵喵！")
    }

    val animals: List<Animal> = listOf(Dog(), Cat(), Dog())

    for (animal in animals) {
        if (animal is Dog) {
            // 智能转型：此处animal已自动转为Dog类型，无需手动转换
            animal.bark()
        } else if (animal is Cat) {
            animal.meow()
        }
    }
}

/**
 * 示例三：显式类型转换。
 *
 * 使用"as"进行显式类型转换；使用"as?"进行安全转换（失败时返回null）。
 */
fun example03() {
    val obj: Any = "Hello, Kotlin!"

    // 使用as进行强制类型转换
    val str1: String = obj as String
    println("强制转换结果：$str1")

    // 使用as?进行安全转换
    val num: Any = 42
    val str2: String? = num as? String
    println("安全转换结果：$str2")  // 输出null，不会抛出异常
}
