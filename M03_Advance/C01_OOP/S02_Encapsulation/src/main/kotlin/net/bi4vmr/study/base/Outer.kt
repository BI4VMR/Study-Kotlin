package net.bi4vmr.study.base

/**
 * 内部类示例：外部类。
 *
 * Kotlin中需要使用"inner"关键字声明成员内部类，才能访问外部类的成员。
 * 不带"inner"关键字的嵌套类等同于Java的静态内部类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Outer {
    // 外部类的私有属性name
    private val name = "外部类"
    // 外部类的成员属性
    val age = 20

    // 成员内部类Inner（需要"inner"关键字才能访问外部类成员）
    inner class Inner {
        val name = "内部类"

        // 内部类中的方法
        fun show() {
            println("外部类中的name：${this@Outer.name}")
            println("内部类中的name：$name")
            println("外部类中的age：$age")
        }
    }

    // 嵌套类（等同于Java的静态内部类，不能访问外部类的非静态成员）
    class NestedClass {
        fun show() {
            println("这是嵌套类（静态内部类）")
        }
    }
}
