package net.bi4vmr.study.oop.base

/**
 * 示例类：人类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Person2 constructor(
    name: String,
    age: Int
) {

    /* 属性 */
    var name: String = ""
    var age: Int = 0

    /* 初始化块 */
    init {
        println("使用主要构造方法初始化对象...")
        this.name = name
        this.age = age
    }

    /* 方法 */
    fun speak() {
        println("我是${name}，年龄${age}岁。")
    }
}
