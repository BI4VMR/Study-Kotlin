package net.bi4vmr.study.base

/**
 * 动物接口的实现类：猫。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Cat : Animal {

    override fun eat() {
        // 实现"eat()"方法的逻辑
        println("猫吃猫粮")
    }
}
