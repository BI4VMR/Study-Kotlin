package net.bi4vmr.study.lambda

import java.util.function.Consumer

/**
 * 示例代码：基本应用。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example02()
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
    val list = mutableListOf(1, 2, 3, 4, 5)


    // 设置排序规则，将所有元素降序排列。
    list.sortWith({ o1, o2 -> o2.compareTo(o1) })

    // Lambda优化：如果Lambda表达式是函数的最后一个参数，可以将其移到括号外面。
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
 * 示例六： `this` 关键字。
 * <p>
 * 在本示例中，我们比较匿名内部类与Lambda表达式中 `this` 关键字的含义。
 */
fun example05() {
    Test().test()
}

class Test() {
    fun test() {
        // 将Lambda表达式的引用保存在变量中
        val task: () -> Unit = { println(this) }
        task.invoke()


        val c: Consumer<String> = object : Consumer<String> {

            // 匿名内部类可以拥有属性
            private val name = "Consumer"

            override fun accept(t: String) {
                // `this` 指向匿名内部类实例，可以访问其属性。
                println(this.name)
            }
        }
        c.accept("Hello, World!")
    }
}
