package net.bi4vmr.study.lambda

/**
 * 示例代码：基本应用。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example05()
}


/**
 * 示例一：将整数列表中的元素倒序排列。
 *
 * 在本示例中，我们定义一个整数列表，并将其中的元素倒序排列。
 */
fun example01() {
    // 创建测试列表
    val list = mutableListOf(1, 2, 3, 4, 5)

    // 设置排序规则，将所有元素降序排列。
    list.sortWith(object : Comparator<Int> {

        override fun compare(o1: Int, o2: Int): Int {
            // 返回当前两个数值的比较结果
            return o2.compareTo(o1)
        }
    })

    // 输出排序后的结果
    list.forEach { println(it) }
}


/**
 * 示例二：将整数列表中的元素倒序排列（使用Lambda表达式）。
 *
 * 在本示例中，我们定义一个整数列表，并将其中的元素倒序排列。
 */
fun example02() {
    // 创建测试列表
    val list = mutableListOf(1, 2, 3, 4, 5, 0)


    // 设置排序规则，将所有元素降序排列。
    list.sortWith({ o1, o2 -> o2.compareTo(o1) })

    // Lambda优化：如果Lambda表达式是方法的最后一个参数，可以将其移到括号外面。
    list.sortWith { o1, o2 -> o2.compareTo(o1) }


    // 输出排序后的结果
    list.forEach { println(it) }
}


/**
 * 示例三：方法引用。
 * <p>
 * 在本示例中，我们使用方法引用简化Lambda表达式。
 */
fun example03() {
    // 创建测试列表
    val list = mutableListOf(1, 2, 3, 4, 5)


    // 将Lambda参数传递给 `reverse()` 方法，并返回其结果。
    list.sortWith { o1, o2 -> reverse(o1, o2) }

    // 此时可以简化为方法引用形式： `::<方法名称>` 。
    list.sortWith(::reverse)

    // 如果是引用对象的方法，则需要使用 `<对象名称>::<方法名称>` 或 `this::<方法名称>` 的形式。


    // 输出排序后的结果
    list.forEach { println(it) }
}

/**
 * 比较两个整数的大小，返回倒序排列的结果。
 *
 * @param[a] 第一个整数。
 * @param[b] 第二个整数。
 * @return 比较结果。
 */
fun reverse(a: Int, b: Int): Int {
    return b.compareTo(a)
}


/**
 * 示例四：引用Lambda表达式。
 * <p>
 * 在本示例中，我们使用方法引用简化Lambda表达式。
 */
fun example04() {
    // 将Lambda表达式的引用保存在变量中
    val task: () -> Unit = { println("Hello, World!") }

    // 创建线程，复用Lambda表达式。
    Thread(task).start()
    Thread(task).start()
}


/**
 * 示例五： `this` 关键字。
 * <p>
 * 在本示例中，我们辨析Lambda表达式与匿名内部类中 `this` 关键字的区别。
 */
fun example05() {
    Test().testThis()
}

class Test() {

    fun testThis() {
        // 测试类实例的 `this` 引用
        println(this)


        // Lambda表达式中的 `this` 指向包含它的实例，即 `TestBase` 的实例。
        val lambda: () -> Unit = { println("this in lambda: $this") }
        lambda.invoke()


        // 匿名内部类中的 `this` 指向类的实例，可以访问其属性。
        val object1: Runnable = object : Runnable {

            // 匿名内部类可以拥有属性
            private val name = "Runnable"

            override fun run() {
                println("this in annonymous class: $this")
                // `this` 可以访问匿名内部类的属性
                println("get name by this: ${this.name}")
            }
        }
        object1.run()
    }
}


/**
 * 示例七：捕获外部变量。
 * <p>
 * 在本示例中，我们尝试在Lambda表达式中访问和修改外部变量。
 */
fun example06() {
    Test2().testCapture()
}

class Test2 {

    fun testCapture() {
        // 定义一个局部变量
        var num = 0

        // 定义Lambda表达式，尝试访问和修改外部变量。
        val lambda: () -> Unit = {
            // 访问外部变量
            println("Read `num` in lambda:[$num]")

            // 修改外部变量
            num++
        }

        // 执行Lambda表达式
        lambda()

        // 在Lambda表达式外部访问修改后的变量值
        println("Read `num` outside lambda:[$num]")
    }
}
