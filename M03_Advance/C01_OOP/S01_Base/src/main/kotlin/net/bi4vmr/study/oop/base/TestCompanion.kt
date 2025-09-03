package net.bi4vmr.study.oop.base

/**
 * 测试代码：伴生对象。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example00()
}


/**
 * 示例二：全局变量与默认值。
 * <p>
 * 在本示例中，我们定义一个测试类并声明若干全局变量，并在控制台上输出它们的值。
 */
fun example00() {
    // 访问伴生对象中的属性
    println("访问属性：${TestCompanion.x}")

    // 访问伴生对象中的方法
    println("访问方法：${TestCompanion.avg(6, 4)}")
}

class TestCompanion {

    // 伴生对象
    companion object {
        // 声明变量
        @JvmStatic
        val x: String = "TEST"

        // 声明方法
        @JvmStatic
        fun avg(a: Int, b: Int): Double {
            return ((a + b) / 2.0)
        }
    }
}
