package net.bi4vmr.study

/**
 * 测试代码：方法。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example07()
}

// ======================== 方法定义 ========================

/**
 * 定义方法：计算两数之和（无返回值）。
 *
 * @param a 运算数A。
 * @param b 运算数B。
 */
fun plus(a: Int, b: Int) {
    val sum = a + b
    println("$a + $b = $sum")
}

/**
 * 计算两数之和（有返回值）。
 *
 * @param a 运算数A。
 * @param b 运算数B。
 * @return 两数之和。
 */
fun plus2(a: Int, b: Int): Int {
    val sum = a + b
    return sum
}

/**
 * 计算三数之和（重载方法）。
 *
 * @param a 运算数A。
 * @param b 运算数B。
 * @param c 运算数C。
 * @return 三数之和。
 */
fun plus2(a: Int, b: Int, c: Int): Int {
    val sum = a + b + c
    return sum
}

/**
 * 打招呼（含默认参数）。
 *
 * @param name  对象名称，默认为"World"。
 * @param times 重复次数，默认为1。
 */
fun greet(name: String = "World", times: Int = 1) {
    repeat(times) {
        println("Hello, $name!")
    }
}

/**
 * 创建用户（演示具名参数）。
 *
 * @param name  用户名。
 * @param age   年龄。
 * @param email 邮箱地址。
 */
fun createUser(name: String, age: Int, email: String) {
    println("Name: $name, Age: $age, Email: $email")
}

/**
 * 计算若干整数之和（可变参数）。
 *
 * @param args 运算数列表。
 * @return 所有运算数之和。
 */
fun calculateSum(vararg args: Int): Long {
    println("输入参数为：${args.toList()}")
    var sum = 0L
    for (arg in args) {
        sum += arg
    }
    return sum
}

/**
 * 保存文件（模拟异步操作，通过Lambda回调通知结果）。
 *
 * @param callback 回调函数，参数为(执行结果, 消息)。
 */
fun saveFile(callback: (Boolean, String) -> Unit) {
    Thread {
        Thread.sleep(2000L)
        // 异步操作完成，触发回调。
        callback(true, "OK.")
    }.start()
}

/**
 * 内联方法：测量代码块的执行时间。
 *
 * @param block 待测量的代码块。
 * @return 代码块执行耗时（毫秒）。
 */
inline fun measureTime(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

// ======================== 示例方法 ========================

/**
 * 示例一：参数的基本应用。
 *
 * 在本示例中，我们定义一个方法，接收两个整型参数，功能是将它们的值累加求和，并将结果输出到控制台上。
 */
fun example01() {
    val x = 8
    val y = 6

    // 调用带参数无返回值方法，依次传入各参数。
    plus(x, y)
}

/**
 * 示例二：返回值的基本应用。
 *
 * 在本示例中，我们定义一个带返回值的方法，通过返回值汇报计算结果。
 */
fun example02() {
    val x = 8
    val y = 6

    // 调用带返回值方法，使用一个变量接收返回值。
    val summary = plus2(x, y)
    println("两数之和：$summary")
}

/**
 * 示例三：方法的重载。
 *
 * 在本示例中，我们对前文"示例二"进行扩充，新增计算三个数之和的方法。
 */
fun example03() {
    val x = 8
    val y = 6
    val z = 10

    // 调用具有两个参数的"plus2()"方法
    val sum1 = plus2(x, y)
    println("两数之和：$sum1")
    // 调用具有三个参数的"plus2()"方法
    val sum2 = plus2(x, y, z)
    println("三数之和：$sum2")
}

/**
 * 示例四：默认参数的基本应用。
 *
 * 在本示例中，我们定义一个方法，其中部分参数具有默认值，演示不同调用方式的效果。
 */
fun example04() {
    // 不传参数，全部使用默认值
    greet()
    // 只传第一个参数
    greet("Kotlin")
    // 传入全部参数
    greet("Alice", 2)
}

/**
 * 示例五：具名参数的基本应用。
 *
 * 在本示例中，我们使用具名参数方式调用方法，演示参数顺序与可读性的提升。
 */
fun example05() {
    // 使用具名参数，顺序可以与定义不同
    createUser(
        age = 25,
        email = "alice@example.com",
        name = "Alice"
    )
}

/**
 * 示例六：可变参数的基本应用。
 *
 * 在本示例中，我们通过可变参数实现整数求和方法。
 */
fun example06() {
    // 传递可变参数（通常用法）
    val result1 = calculateSum(1, 2, 3)
    println("Summary is $result1.")

    // 使用展开运算符将数组展开为可变参数
    val arr = intArrayOf(1, 2, 3)
    val result2 = calculateSum(*arr)
    println("Summary is $result2.")

    // 传递可变参数（0个参数）
    val result3 = calculateSum()
    println("Summary is $result3.")
}

/**
 * 示例七：回调方法的基本应用。
 *
 * 在本示例中，我们定义一个方法，接收Lambda表达式作为回调，并在异步操作完成后触发。
 */
fun example07() {
    println("Test method start.")
    // 调用方法，传入Lambda表达式作为回调。
    saveFile { result, message ->
        println("Callback triggered. Result: $result, Message: $message")
    }
    println("Test method end.")
    // 等待异步线程结束
    Thread.sleep(3000L)
}

/**
 * 示例八：内联方法的基本应用。
 *
 * 在本示例中，我们使用内联方法测量代码块的执行时间。
 */
fun example08() {
    val elapsed = measureTime {
        // 模拟耗时操作
        var sum = 0L
        for (i in 1..1_000_000) {
            sum += i
        }
        println("计算完毕")
    }
    println("耗时：${elapsed}ms")
}
