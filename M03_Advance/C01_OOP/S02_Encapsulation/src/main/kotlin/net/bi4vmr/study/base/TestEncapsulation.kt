package net.bi4vmr.study.base

/**
 * 测试代码：封装。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example04()
}

/**
 * 示例一：使用data class访问属性。
 *
 * Kotlin的data class自动提供Getter/Setter（通过val/var），
 * 无需像Java那样手写get/set方法。
 */
fun example01() {
    val student = Student()
    // 直接赋值（等同于Java的setter）
    student.name = "张三"
    // 直接读取（等同于Java的getter）
    println("name的值为：${student.name}")
}

/**
 * 示例二：data class自动生成的方法。
 *
 * data class自动生成toString、equals、hashCode等方法。
 */
fun example02() {
    val s1 = Student(id = "001", name = "张三")
    val s2 = Student(id = "001", name = "张三")

    // toString()
    println("对象信息：$s1")
    // equals()：比较内容而非引用
    println("equals比较：${s1 == s2}")
    // 引用比较
    println("引用比较：${s1 === s2}")
    // copy()：浅拷贝并可选修改部分属性
    val s3 = s1.copy(name = "李四")
    println("拷贝后：$s3")
}

/**
 * 示例三：成员内部类。
 *
 * 使用"inner"关键字声明的内部类可以访问外部类的成员。
 */
fun example03() {
    // 创建外部类的对象
    val outer = Outer()
    // 创建内部类的对象
    val inner = outer.Inner()
    // 调用内部类对象的show方法
    inner.show()
}

/**
 * 示例四：匿名对象（对应Java的匿名内部类）。
 *
 * Kotlin使用"object : 接口/类"语法创建匿名对象。
 */
fun example04() {
    // 使用匿名对象实现接口
    USBManager.addUSBStateCallback(object : USBManager.USBStateCallback {
        override fun onPlugged() {
            println("检测到USB设备插入！")
        }

        override fun onUnPlugged() {
            println("检测到USB设备拔出！")
        }
    })
}
