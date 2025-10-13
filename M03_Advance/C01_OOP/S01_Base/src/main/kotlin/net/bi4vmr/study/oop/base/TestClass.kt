package net.bi4vmr.study.oop.base

/**
 * 测试代码：类与对象。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example06()
}


/**
 * 示例一：面向对象的基本应用。
 *
 * 在本示例中，我们定义Person类描述“人类”，并创建一些对象。
 */
fun example01() {
    // 从模板“人类”创建实体“张三”
    val zhangsan = Person()
    // 设置属性
    zhangsan.name = "张三"
    zhangsan.age = 18
    zhangsan.sex = '男'
    // 调用方法
    zhangsan.speak()

    // 从模板“人类”创建实体“李四”
    val lisi = Person()
    lisi.name = "李四"
    lisi.age = 20
    lisi.sex = '女'
    lisi.speak()
}


/**
 * 示例四：使用主要构造方法初始化对象。
 *
 * 在本示例中，我们对前文“示例一”所定义的Person类进行修改，为构造方法添加姓名、年龄两个参数，并为对应的全局变量赋值，完成对象的初始化。
 */
fun example04() {
    // 使用主要构造方法创建对象
    val zhangsan = Person2("张三", 18)
    zhangsan.speak()
}


/**
 * 示例五：在主要构造方法中声明全局变量。
 *
 * 在本示例中，我们对前文“示例四”所定义的Person类进行修改，在构造方法中定义与参数同名的全局变量。
 */
fun example05() {
    // 创建对象，可选参数使用默认值。
    val lisi = Person3("李四", 20)
    lisi.speak()

    // 创建对象，可选参数使用指定值。
    val alice = Person3("Alice", 22, '女')
    alice.speak()
}


/**
 * 示例六：使用次要构造方法初始化对象。
 *
 * 在本示例中，我们对前文“示例五”所定义的Person类进行修改，添加次要构造方法。
 */
fun example06() {
    // 使用次要构造方法创建对象
    val lisi = Person4("李四")
    lisi.speak()
}
