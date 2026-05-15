package net.bi4vmr.study.base

/**
 * 接口示例：动物。
 *
 * Kotlin的接口与Java类似，还支持为方法提供默认实现。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface Animal {

    // 定义常量（接口中属性默认是abstract）
    val typeName: String
        get() = "Animal"

    // 定义抽象方法（接口方法默认为abstract）
    fun eat()

    // 定义带默认实现的方法（Kotlin接口独有特性）
    fun breathe() {
        println("$typeName 在呼吸...")
    }
}
