package net.bi4vmr.study.base

/**
 * 示例类：子类。
 *
 * Kotlin使用": 父类()"语法继承父类（需要调用父类构造方法）。
 * 重写父类方法需要使用"override"关键字。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Child : Father() {

    override fun show() {
        println("This is Child.")
    }
}
