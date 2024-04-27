package net.bi4vmr.study.classandobjects

/**
 * 测试代码 - 类与对象。
 *
 * @author BI4VMR
 */
fun main() {
    example03()
}

/*
 * 示例：由类创建对象，并访问对象的属性与方法。
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

/*
 * 示例：使用主要构造方法初始化对象。
 */
fun example02() {
    // 使用主要构造方法创建对象
    val zhangsan = Person2("张三", 18)
    zhangsan.speak()
}

/*
 * 示例：使用次要构造方法初始化对象。
 */
fun example03() {
    // 使用次要构造方法创建对象
    val lisi = Person3("李四", 20, '女')
    lisi.speak()
}
