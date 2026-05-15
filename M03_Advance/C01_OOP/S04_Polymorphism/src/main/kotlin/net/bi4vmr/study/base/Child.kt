package net.bi4vmr.study.base

/**
 * 示例类：子类（多态示例）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Child : Father() {

    override fun show() {
        println("This is Child.")
    }
}
