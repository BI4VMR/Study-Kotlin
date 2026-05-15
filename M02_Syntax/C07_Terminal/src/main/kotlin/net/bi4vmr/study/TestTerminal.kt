package net.bi4vmr.study

/**
 * 测试代码：终端交互。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example05()
}

/**
 * 示例一：读取控制台输入的基本应用。
 *
 * 在本示例中，我们从控制台接收两个数字，并将它们的和输出到控制台上。
 */
fun example01() {
    println("请输入第一个数字，按回车确认：")
    val num1 = readlnOrNull()?.toDoubleOrNull() ?: 0.0

    println("请输入第二个数字，按回车确认：")
    val num2 = readlnOrNull()?.toDoubleOrNull() ?: 0.0

    // 求和并输出结果
    val sum = num1 + num2
    print("$num1 + $num2 = $sum")
}

/**
 * 示例二：控制台输出方法的基本应用。
 *
 * 在本示例中，我们使用控制台输出方法显示变量的值。
 */
fun example02() {
    val i = 100
    val s = "Hello"

    // 输出整型值
    print(i)
    // 输出空格
    print(" ")
    // 输出字符串并换行
    println(s)

    // 输出组合后的字符串（使用字符串模板）
    println("i = $i")
}

/**
 * 示例三：格式化输出方法的基本应用。
 *
 * 在本示例中，我们使用格式化输出方法向控制台输出文本信息。
 */
fun example03() {
    val i = 127
    val c = 'A'
    val s = "ABC"

    // 将i输出为十进制
    print("%d ".format(i))
    // 将i输出为八进制
    print("%o ".format(i))
    // 将i输出为十六进制
    print("%x ".format(i))
    // 将i输出为十六进制，并附加"0x"前缀。
    print("%#x ".format(i))
    // 换行
    println()
    // 输入多个参数
    print("i = %d; c = %c; s = %s".format(i, c, s))
}

/**
 * 示例四：格式化输出小数。
 *
 * 在本示例中，我们使用格式化输出方法控制小数的位数。
 */
fun example04() {
    val d = 12.3456789

    // 默认情况下保留6位小数
    println("%f".format(d))

    // 保留两位小数
    println("%.2f".format(d))

    // 保留两位后长度为6，所以左侧补一个空格
    print("%6.2f".format(d))
}

/**
 * 示例五：转义字符的基本应用。
 *
 * 在本示例中，我们使用转义字符控制输出文本的格式。
 */
fun example05() {
    val s = "A\tB\tC\tD\nE\tF\tG\tH\nI\tJ\tK\tL"
    println(s)
}
