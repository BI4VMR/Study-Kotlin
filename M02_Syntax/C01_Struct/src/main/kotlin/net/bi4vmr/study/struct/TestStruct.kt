package net.bi4vmr.study.struct
//
// fun main() {
//     example06()
// }
//
// /*
//  * 示例：空值安全
//  */
// fun example01() {
//     // 声明非空字符串"str1"
//     var str1: String = "字符串内容"
//     // 声明可空字符串"str2"
//     var str2: String? = null
//
//     // 将空值赋予非空变量，该语句无法通过编译。
//     // str1 = null
//     // 将空值赋予可空变量，该语句可以通过编译。
//     str2 = null
//
//     // 调用无空变量的属性，该语句可以通过编译。
//     str1.length
//     // 调用可空变量的属性，该语句无法通过编译。
//     // str2.length
// }
//
// /*
//  * 示例：安全调用操作符
//  */
// fun example02() {
//     var str1: String? = "字符串内容"
//     var str2: String? = null
//
//     // 使用安全调用操作符访问两个变量
//     println("str1的内容：" + str1?.length)
//     println("str2的内容：" + str2?.length)
// }
//
// /*
//  * 示例：判断空值
//  */
// fun example03() {
//     var str: String? = null
//
//     // 判断字符串是否为空值
//     if (str != null) {
//         // 当变量不为空时，再访问其中的属性与方法。
//         println(str.length)
//     }
//
//     // 上述逻辑的等价写法
//     str?.let {
//         // 变量"it"等同于"str"
//         println(it)
//     }
// }
//
// /*
//  * 示例：级联判空
//  */
// fun example04() {
//     // 获取学生的所有课程
//     // val courses = student.getCourses()
//     // if (courses != null) {
//     //     // 获取最新选择的课程
//     //     val latestCourse = courses.getLatestCourse()
//     //     if (latestCourse != null) {
//     //         // 获取课程的学分
//     //         val credit = latestCourse.getCredit()
//     //         println("学分：$credit")
//     //     }
//     // }
//
//     // 上述逻辑的等价写法
//     // val credit = student?.getCourses()?.getLatestCourse()?.getCredit()
//     // println("学分：$credit")
// }
//
// /*
//  * 示例：Elvis运算符
//  */
// fun example05() {
//     var str1: String? = "字符串内容"
//     var str2: String? = null
//
//     val length1: Int = str1?.length ?: 0
//     val length2: Int = str2?.length ?: 0
//
//     println("str1的长度:$length1")
//     println("str2的长度:$length2")
//
//     // 在Elvis运算符中使用"return"和"throw"语句
//     // fun getLength(str: String?): Int {
//     //     str?.length ?: return 0
//     //     str?.length ?: throw IllegalArgumentException("入参为空值！")
//     // }
// }
//
// /*
//  * 示例：非空断言
//  */
// fun example06() {
//     var str: String? = null
//
//     // 该语句无法通过编译。
//     // println(str.length)
//
//     // 该语句可以通过编译，但运行时将会出现NPE。
//     println(str!!.length)
// }
