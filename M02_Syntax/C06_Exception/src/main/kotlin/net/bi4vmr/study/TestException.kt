package net.bi4vmr.study

import net.bi4vmr.study.exception.CustomException
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.net.SocketException
import java.util.*

/**
 * 测试代码 - 异常。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example06()
}

/**
 * 示例一：异常捕获语句的基本应用。
 *
 * 在本示例中，我们将两个整数相除的语句放置在 `try-catch-finally` 语句中，使用不同的值多次运行程序，对比程序的行为。
 */
fun example01() {
    try {
        val i = 10 / 0
        println("i=$i")
    } catch (e: java.lang.Exception) {
        println("发生异常！")
    } finally {
        println("善后工作完成！")
    }
    println("整个程序已结束！")
}

/**
 * 示例二：在控制台上显示异常详情。
 *
 * 在本示例中，我们故意制造一个异常，捕获异常后使用 `printStackTrace()` 方法将详情输出到控制台上。
 */
fun example02() {
    try {
        val i = 10 / 0
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 示例三：捕获多种类型的异常。
 *
 * 在本示例中，我们使用多个 `catch` 块，分别捕获不同类型的异常。
 */
fun example03() {
    try {
        val i = 10 / 0
    } catch (ae: ArithmeticException) {
        println("除数不能为0！")
    } catch (e: Exception) {
        println("其它错误。")
    }
}

/**
 * 示例四：异常与 `return` 语句。
 *
 * 在本示例中，我们故意制造一个异常，然后在 `try-catch-finally` 语句中添加 `return` 语句，测试跳转规则。
 */
fun example04() {
    val result = returnInTryCatch()
    println(result)
}

fun returnInTryCatch(): String {
    try {
        10 / 0
        return "try块中的return生效了。"
    } catch (e: Exception) {
        // 出现异常情况的返回值
        return "catch块中的return生效了。"
    } finally {
        return "finally块中的return生效了。"
    }
}

/**
 * 示例五： `runCatching()` 函数的基本应用。
 *
 * 在本示例中，我们使用 `runCatching()` 方法代替 `try-catch` 语法。
 */
fun example05() {
    runCatching {
        val i = 10 / 5
        println("i=$i")
        // 返回一个数值
        i
    }.onSuccess { result ->
        println("操作完毕，未发生异常！")
        println("接收到返回值：$result")
    }.onFailure { e ->
        println("发生异常！${e.message}")
    }
}

/**
 * 示例六： `use()` 函数的基本应用。
 *
 * 在本示例中，我们从控制台读取一行文本，并将其写入文件。
 */
fun example06() {
    val scanner = Scanner(System.`in`)
    scanner.use {
        BufferedWriter(FileWriter("output.txt")).use {
            println("请输入文本，按回车键确认：")
            val text = scanner.nextLine()
            it.write(text)
        }
    }
}

/**
 * 示例七：主动抛出异常。
 *
 * 在本示例中，我们编写一个方法用于计算整数角度的正切值，当输入值为90度的倍数时，向调用者抛出算术异常。
 */
fun example07() {
    try {
        println("tan(45) = ${tan(45)}")
        println("tan(90) = ${tan(90)}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun tan(deg: Int): Double {
    if (deg % 90 == 0) {
        throw ArithmeticException()
    } else {
        return kotlin.math.tan(Math.toRadians(deg.toDouble()))
    }
}

/**
 * 示例八：定义并使用自定义异常。
 *
 * 在本示例中，我们创建一个自定义异常类，在测试方法中抛出并捕获该异常。
 */
fun example08() {
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
 * 示例九：异常链的基本应用。
 *
 * 在本示例中，我们故意制造一个算术异常，并将其捕获后转为前文“示例七”中的CustomException，再向调用者抛出。
 */
fun example09() {
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

/**
 * 示例十：与Java交互 - 声明方法可能抛出的异常。
 *
 * 在本示例中，我们自定义一个方法，并使用 `@Throws` 注解声明可能出现的异常。
 */
@Throws(IOException::class, SocketException::class)
fun javaExceptionTest() {
    throw IOException()
    // throw SocketException()
}
