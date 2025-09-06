package net.bi4vmr.study.oop.base

/**
 * 示例类：人类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Person3 constructor(
    name: String,
    age: Int
) {

    /* 属性 */
    var name: String = ""
    var age: Int = 0
    var sex: Char = '男'

    /* 初始化块 */
    init {
        println("初始化块...")
        this.name = name
        this.age = age
    }

    /* 次要构造方法 */
    constructor(name: String, age: Int, sex: Char) : this(name, age) {
        println("次要构造方法...")
        this.sex = sex
    }

    /* 方法 */
    fun speak() {
        println("我是${name}，年龄${age}岁，性别为${sex}")
    }
}
