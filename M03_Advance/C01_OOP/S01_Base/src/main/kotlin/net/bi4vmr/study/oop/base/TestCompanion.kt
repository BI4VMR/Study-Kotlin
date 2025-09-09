package net.bi4vmr.study.oop.base

/**
 * 测试代码：伴生对象。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example07()
}


/**
 * 示例七：伴生对象的基本应用。
 *
 * 在本示例中，我们定义测试类并声明伴生对象，然后访问其中的变量与方法。
 */
fun example07() {
    // 访问伴生对象中的属性
    println("访问属性：${TestCompanion.x}")

    // 访问伴生对象中的方法
    println("访问方法：${TestCompanion.avg(6, 4)}")
}

class TestCompanion {

    // 声明伴生对象
    companion object {

        // 声明变量
        val x: String = "TEST"

        // 声明方法
        fun avg(a: Int, b: Int): Double {
            return ((a + b) / 2.0)
        }
    }
}

/**
 * 示例八："c"的基本应用。
 *
 * 在本示例中，我们定义测试类并声明伴生对象，然后访问其中的变量与方法。
 */
fun example08() {
    // 访问伴生对象中的属性
    println("访问属性：${TestCompanion.x}")
}

class TestCompanion2 {

    // 声明伴生对象
    companion object {

        // 声明变量
        val x: String = "TEST"

        // 声明常量
        const val PI: Float = 3.141593F
    }
}
