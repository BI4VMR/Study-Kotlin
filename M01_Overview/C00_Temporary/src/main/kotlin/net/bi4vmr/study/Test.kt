package net.bi4vmr.study

fun main() {
    example03()
}

/**
 * 示例：retrun
 */
fun example01() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    array.forEach {
        if (it == 2) {
            // 返回foreach之外
            return
        }

        println("Loop $it")
    }

    println("Some statement after loop...")
}

fun example02() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    array.forEach {
        if (it == 2) {
            // 相当于"for"循环中的"continue"
            return@forEach
        }

        println("Loop $it")
    }

    println("Some statement after loop...")
}

/**
 * 示例：类似break
 */
fun example03() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    run loop@ {
        array.forEach {
            if (it == 3) {
                // 相当于"for"循环中的"break"
                return@loop
            }

            println("Loop $it")
        }
    }

    println("Some statement after loop...")
}
