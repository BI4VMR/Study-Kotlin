package net.bi4vmr.study.oop.base

/**
 * 类的示例：人类。
 *
 * @author BI4VMR
 */
class Person4 constructor(
    val name: String,
    val age: Int,
    private val sex: Char
) {

    /* 方法 */
    fun speak() {
        println("我是${name}，年龄${age}岁，性别为${sex}")
    }
}
