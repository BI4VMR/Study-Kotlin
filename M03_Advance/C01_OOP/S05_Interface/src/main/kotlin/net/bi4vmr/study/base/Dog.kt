package net.bi4vmr.study.base

/**
 * 动物接口的实现类：狗。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Dog : Animal {

    override fun eat() {
        // 访问接口中的属性
        println("类型：$typeName")
        // 实现"eat()"方法的逻辑
        println("狗吃狗粮")
    }
}
