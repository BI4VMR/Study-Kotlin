package net.bi4vmr.study.oop.base

/**
 * 示例代码：伴生对象。
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
 * 示例八："const"关键字。
 *
 * 在本示例中，我们比较"const"常量与普通常量的区别。
 */
fun example08() {
    // 访问伴生对象中的属性
    println("访问常量：${TestCompanion2.PI}")
    println("访问常量：${TestCompanion2.X}")
}

class TestCompanion2 {

    companion object {

        // 声明"const"常量
        const val PI: Float = 3.141593F

        // 声明普通常量
        val X: String = "TEST"
    }
}

// 上述代码在JVM中反编译的结果：
//
// public final class TestCompanion2 {
//
//     // "const"常量
//     public static final float PI = 3.141593F;
//
//     // 普通常量
//     private static final String X = "TEST";
//
//     public static final class Companion {
//         public final String getX() {
//             return TestCompanion2.X;
//         }
//     }
// }


/**
 * 示例九："@JvmStatic"注解。
 *
 * 在本示例中，我们为变量与方法添加 `@JvmStatic` 注解，并在Java代码中访问它们。
 */
class TestCompanion3 {

    companion object {

        @JvmStatic
        val x: String = "TEST"

        @JvmStatic
        fun avg(a: Int, b: Int): Double {
            return ((a + b) / 2.0)
        }
    }
}
