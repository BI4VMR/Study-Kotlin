package net.bi4vmr.study.classandobjects

/**
 * 测试代码 - 伴生对象。
 *
 * @author BI4VMR
 */
fun main() {
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
