package net.bi4vmr.study.abstractclass

/**
 * 动物抽象类的子类：狗。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Dog : Animal() {

    override fun eat() {
        // 实现"eat()"方法的逻辑
        println("狗吃狗粮")
    }
}
