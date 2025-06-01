package net.bi4vmr.study

import net.bi4vmr.study.exception.CustomException

/**
 * 测试代码 - 异常。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    try {
        convertException()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 示例：变量的基本应用。
 *
 * 在本示例中，我们定义一些变量，并访问它们。
 */
fun example02() {
    // 声明变量“姓名”
    val name: String = "张三"
    // 声明变量“年龄”
    val age: Int = 20

    // 访问变量：将变量的值输出到控制台
    println("姓名：$name")
    println("年龄：$age")
}

/**
 * 示例七：定义并使用自定义异常。
 *
 * 在本示例中，我们创建一个自定义异常类，在测试方法中抛出并捕获该异常。
 */
fun example07() {
    try {
        raiseException()
    } catch (e: CustomException) {
        println("遇到异常：${e.message}")
        println("错误码：${e.getCode()}")
    }
}

fun raiseException() {
    throw CustomException(100, "自定义异常")
}

/**
 * 示例八：异常链的基本应用。
 *
 * 在本示例中，我们故意制造一个算术异常，并将其捕获后转为前文“示例七”中的CustomException，再向调用者抛出。
 */
fun example08() {
    try {
        convertException()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun convertException() {
    try {
        10 / 0
    } catch (ae: ArithmeticException) {
        println("捕获算术异常，将其转换为自定义异常再向上层抛出。")
        val customException = CustomException(100, "自定义异常")
        // 使用 `initCause()` 方法指明引起CustomException的原始异常
        customException.initCause(ae)
        // 抛出新建的CustomException
        throw customException
    }
}
