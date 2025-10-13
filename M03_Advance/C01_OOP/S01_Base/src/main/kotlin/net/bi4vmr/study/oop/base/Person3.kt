package net.bi4vmr.study.oop.base

/**
 * 示例类：人类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Person3 constructor(
    val name: String,
    val age: Int,
    private val sex: Char = '男'
) {

    /* 方法 */
    fun speak() {
        println("我是${name}，年龄${age}岁，性别为${sex}。")
    }
}
