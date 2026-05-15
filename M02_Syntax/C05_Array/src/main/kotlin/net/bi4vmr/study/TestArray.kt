package net.bi4vmr.study

/**
 * 测试代码：数组。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example08()
}

/**
 * 基本应用：声明并访问数组。
 */
fun example01() {
    // 定义字符串数组，保存考试科目信息
    val subjects = arrayOf("Oracle", "PHP", "Linux", "Kotlin", "HTML")

    // 访问第四个元素（下标从0开始）
    println("数组中第4个科目为：${subjects[3]}")
}

/**
 * 初始化数组。
 */
fun example02() {
    // 创建包含三个元素的整型数组
    val array = IntArray(3)
    // 查看元素的默认值
    print("默认值：")
    print("${array[0]} ")
    print("${array[1]} ")
    println(array[2])

    // 初始化数组，将所有元素的值设为100。
    array.fill(100)
    // 查看填充后各元素的值
    print("填充后：")
    print("${array[0]} ")
    print("${array[1]} ")
    print(array[2])
}

/**
 * 使用循环语句操作数组。
 */
fun example03() {
    // 定义一个字符串数组。
    val hobbys = arrayOf("sports", "game", "movie")
    println("循环输出数组中元素的值：")
    // 使用for循环遍历数组中的元素
    for (i in hobbys.indices) {
        println(hobbys[i])
    }
}

/**
 * 使用"for-in"操作数组。
 */
fun example04() {
    // 定义一个整型数组，保存成绩信息
    val scores = intArrayOf(89, 72, 64, 58, 93)
    // 使用for-in遍历输出数组中的元素
    for (score in scores) {
        print("$score; ")
    }
}

/**
 * 复制数组（错误示范）。
 */
fun example05() {
    val array1 = intArrayOf(1, 2, 3)
    // 定义"array2"并将"array1"赋值给它（引用复制，非内容复制）
    val array2 = array1
    // 更改"array1"的第一个元素
    array1[0] = 10
    // 输出"array2"的第一个元素，会随"array1"一起变化
    println(array2[0])
}

/**
 * 复制数组（正确示范）。
 */
fun example06() {
    // 创建包含3个元素的整型数组
    val array1 = intArrayOf(1, 2, 3)
    // 复制全部数组
    val array2 = array1.copyOf()
    // 修改array1不影响array2
    array1[0] = 10
    println("array1[0] = ${array1[0]}")
    println("array2[0] = ${array2[0]}")
}

/**
 * 使用扩展方法操作数组：排序与转字符串。
 */
fun example07() {
    val scores = intArrayOf(89, 72, 64, 58, 93)
    // 使用"sort()"方法对数组进行排序
    scores.sort()

    print("升序排列结果：")
    for (score in scores) {
        print("$score;")
    }
    println()

    // 使用"contentToString()"方法将数组转换为字符串
    val str = scores.contentToString()
    print("数组内容：$str")
}

/**
 * 声明并访问二维数组。
 */
fun example08() {
    // 定义2行3列的二维数组并赋值
    val names = arrayOf(
        arrayOf("tom", "jack", "mike"),
        arrayOf("张三", "李四", "王五")
    )
    // 通过二重循环输出二维数组中元素的值
    for (row in names) {
        for (element in row) {
            println(element)
        }
        println()
    }
}
