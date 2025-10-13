package net.bi4vmr.study.oop.base

/**
 * 示例类：人类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Person {

    /* 属性 */
    var name: String = ""
    var age: Int = 0
    var sex: Char = '男'

    /* 方法 */
    fun speak() {
        println("我是${name}，年龄${age}岁，性别为${sex}。")
    }
}
