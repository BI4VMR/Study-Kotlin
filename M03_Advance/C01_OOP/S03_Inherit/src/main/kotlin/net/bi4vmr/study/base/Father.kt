package net.bi4vmr.study.base

/**
 * 示例类：父类。
 *
 * Kotlin中所有类默认是final的，必须使用"open"关键字才能被继承。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
open class Father {

    open fun show() {
        println("This is Father.")
    }
}
