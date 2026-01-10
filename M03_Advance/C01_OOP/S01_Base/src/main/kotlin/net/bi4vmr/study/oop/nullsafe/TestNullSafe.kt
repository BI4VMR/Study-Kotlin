package net.bi4vmr.study.oop.nullsafe

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * 示例代码：空值安全。
 */
fun main() {
    example17()
}


/**
 * 示例十：定义与访问可空变量。
 *
 * 在本示例中，我们定义一些可空变量，并访问它们。
 */
fun example10() {
    // 声明非空字符串 `str1`
    var str1: String = "字符串内容"
    // 声明可空字符串 `str2`
    var str2: String? = null

    // 将空值赋予非空字符串，该语句无法通过编译。
    // str1 = null
    // 将空值赋予可空字符串，该语句可以通过编译。
    str2 = null

    // 调用无空变量的属性，该语句可以通过编译。
    str1.length
    // 调用可空变量的属性，该语句无法通过编译。
    // str2.length
}

/**
 * 示例十一：空值检验。
 *
 * 在本示例中，我们对可空变量进行空值检验，仅当变量非空时访问它的属性与方法。
 */
fun example11() {
    var str: String? = null

    // 判断字符串是否为空值
    if (str != null) {
        // 当变量不为空时，再访问其中的属性与方法。
        println(str.length)
    } else {
        println("对象为空，放弃操作！")
    }
}

/**
 * 示例十二：安全调用操作符。
 *
 * 在本示例中，我们使用安全调用操作符进行空值检验。
 */
fun example12() {
    var str1: String? = "字符串内容"
    var str2: String? = null

    // 使用安全调用操作符访问两个变量
    println("str1的内容：" + str1?.length)
    println("str2的内容：" + str2?.length)
}

/**
 * 示例十三：级联判空（伪代码）。
 *
 * 在本示例中，我们使用安全调用操作符进行级联判空。
 */
/* 普通写法
val courses = student.getCourses()
if (courses != null) {
    // 获取最新选择的课程
    val latestCourse = courses.getLatestCourse()
    if (latestCourse != null) {
        // 获取课程的学分
        val credit = latestCourse.getCredit()
        println("学分：$credit")
    }
}
*/

/* 简化写法
val credit = student?.getCourses()?.getLatestCourse()?.getCredit()
println("学分：$credit")
*/

/**
 * 示例十四：通过安全调用操作符检验空值。
 *
 * 在本示例中，我们使用安全调用操作符进行空值检验。
 */
fun example14() {
    var str: String? = null

    // 判断字符串是否为空值
    if (str != null) {
        // 当变量不为空时，再访问其中的属性与方法。
        println(str.length)
    }

    // 上述逻辑的等价写法
    str?.let {
        // 变量"it"等同于"str"
        println(it)
    }
}

/**
 * 示例十五：Elvis运算符。
 *
 * 在本示例中，我们了解Elvis运算符的应用场景。
 */
fun example15() {
    var str1: String? = "字符串内容"
    var str2: String? = null

    val length1: Int = str1?.length ?: 0
    val length2: Int = str2?.length ?: 0

    println("str1的长度:$length1")
    println("str2的长度:$length2")
}

/**
 * 示例十六：非空断言。
 *
 * 在本示例中，我们了解非空断言的应用场景。
 */
fun example16() {
    // 测试变量，值可以随意修改。
    var str: String? = null
    // 字符串为空时，终止进程。
    runBlocking {
        if (str == null) {
            println("变量为空，终止进程！")
            exitProcess(1)
        }
    }

    // 该语句无法编译通过
    // println("字符数量：${str.length}")

    // 该语句可以编译通过
    println("字符数量：${str!!.length}")
}

/**
 * 示例十七："requireNotNull()"方法。
 *
 * 在本示例中，我们了解 `requireNotNull()` 方法的应用场景。
 */
fun example17() {
    // 测试变量，值可以随意修改。
    var str: String? = "Test String"
    // 字符串为空时，终止进程。
    runBlocking {
        if (str == null) {
            println("变量为空，终止进程！")
            exitProcess(1)
        }
    }

    // 将变量转换为非空变量，并设置变量为空时的错误消息。
    val nonNullStr = requireNotNull(str) { "预期之外的空值，请检查业务逻辑！" }
    // 使用非空变量
    println("字符数量：${nonNullStr.length}")
    println("存在内容？：${nonNullStr.isNotEmpty()}")
}

/**
 * 示例十八：通过 `by lazy {}` 方法延迟加载变量（伪代码）。
 *
 * 在本示例中，我们使用 `by lazy {}` 方法延迟加载非空变量。
 */
/*
class MyProvider : ContentProvider() {

    // 延迟加载非空变量
    private val cacheDir: File by lazy {
        // 该语句将在变量 `cacheDir` 首次被调用时执行。
        requireContext().cacheDir
    }

    // 此方法由系统在创建实例时调用，随后 `requireContext()` 方法可用。
    override fun onCreate() {}

    // 业务方法
    fun listCacheFiles() {
        // 此处为首次访问 `cacheDir` 变量， `by lazy {}` 中的语句被执行。
        cacheDir.list()
    }
}
*/


/**
 * 示例十九：通过 `lateinit` 关键字延迟加载变量（伪代码）。
 *
 * 在本示例中，我们使用 `lateinit` 关键字延迟加载非空变量。
 */
/*
class MyProvider : ContentProvider() {

    // 声明非空变量但先不初始化
    private lateinit var mCacheDir: File

    // 此方法由系统在创建实例时调用，随后 `requireContext()` 方法可用。
    override fun onCreate() {
        // 具备条件时初始化非空变量
        mCacheDir = requireContext().cacheDir
    }

    // 业务方法
    fun listCacheFiles() {
        // `mCacheDir` 变量在系统回调 `onCreate()` 方法时已被初始化，因此可以正常使用。
        mCacheDir.list()

        // 如果不希望访问未初始化的 `lateinit` 变量导致异常，我们也可以主动判断其是否已初始化。
        if (::mCacheDir.isInitialized) {
            println("`lateinit` 变量已初始化。")
        } else {
            println("`lateinit` 变量未初始化，不可访问！")
        }
    }
}
*/
