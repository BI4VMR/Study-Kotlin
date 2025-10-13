package net.bi4vmr.study.reflection.base

import kotlin.reflect.KClass

/**
 * 测试代码：反射。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}

/**
 * 示例一：获取类的Class对象。
 *
 * 在本示例中，我们通过不同的方式获取自定义类Father的Class对象。
 */
fun example01() {
    // 方式一：通过"<类>::class"获取Class对象
    val clazz1: KClass<Animal> = Animal::class

    // 方式二：通过"<对象>.getClass()"方法获取Class对象
    val animal = Animal()
    val clazz2: KClass<out Animal> = animal::class

    // 方式三：通过"<对象>.getClass()"方法获取Class对象
    val clazz3: KClass<out Animal> = animal::class
}

/**
 * 示例一：获取类的Class对象。
 *
 * 在本示例中，我们通过不同的方式获取自定义类Father的Class对象。
 */
fun example02() {
    // 方式一：通过"<类>::class.java"获取Java Class对象
    val clazz1: Class<Animal> = Animal::class.java

    // 方式二：通过"<对象>::class.java"获取Java Class对象
    val animal = Animal()
    val clazz2: Class<Animal> = animal.javaClass
}
