package net.bi4vmr.study.oop.base

/**
 * 类的示例：人类。
 *
 * @author BI4VMR
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
        println("使用有参构造方法初始化对象...")
        this.name = name
        this.age = age
    }

    /* 方法 */
    fun speak() {
        println("我是${name}，年龄${age}岁")
    }
}
