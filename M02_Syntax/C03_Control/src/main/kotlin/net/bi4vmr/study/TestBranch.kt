package net.bi4vmr.study

/**
 * 测试代码：分支结构。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example07()
}

/**
 * 示例一："if"语句的基本应用。
 *
 * 在本示例中，我们给定两个整数，当 `a` 的值比 `b` 的值大时，在控制台上输出结果。
 */
fun example01() {
    val a = 2
    val b = 1

    // "if"的基本结构（简化写法）
    if (a > b) println("a is bigger than b.")

    // "if"的基本结构（完整写法）
    if (a > b) {
        println("a is bigger than b.")
    }
}

/**
 * 示例二："if-else"语句的基本应用。
 *
 * 在本示例中，我们给定一个年龄，当年龄低于18岁时，在控制台上输出文本"未成年人"；否则输出"成年人"。
 */
fun example02() {
    val age = 17
    if (age >= 18) {
        println("成年人")
    } else {
        println("未成年人")
    }
}

/**
 * 示例三：多重"if-else"语句的基本应用。
 *
 * 在本示例中，我们给定一个百分制的成绩，并在控制台上输出成绩所属的等第。
 * 当成绩大于90分时等第为"优"；当成绩属于区间 `(90, 75]` 时等第为"良"；
 * 当成绩属于区间 `(75, 60]` 时等第为"中"，当成绩低于60分时等第为"差"。
 */
fun example03() {
    val score = 60
    print("等第为：")
    if (score >= 90) {
        println("优")
    } else if (score >= 75) {
        println("良")
    } else if (score >= 60) {
        println("中")
    } else {
        println("差")
    }
}

/**
 * 示例四："when"语句的基本应用。
 *
 * 在本示例中，我们给定一个表示季度的整数，范围为：`[1, 4]`，并通过"when"语句将其转换为对应的季度编号。
 */
fun example04() {
    val x = 2
    when (x) {
        1 -> print("Q1")
        2 -> print("Q2")
        3 -> print("Q3")
        4 -> print("Q4")
        else -> print("输入值不合法")
    }
}

/**
 * 示例五：在"when"语句中合并相同的分支。
 *
 * 在本示例中，我们给定一个表示季度的整数，范围为：`[1, 4]`，并通过"when"语句将其转换为"上半年"或"下半年"文本。
 */
fun example05() {
    val x = 1
    when (x) {
        1, 2 -> print("上半年")
        3, 4 -> print("下半年")
        else -> print("输入值不合法")
    }
}

/**
 * 示例六：在"when"语句中使用区间匹配。
 *
 * 在本示例中，我们给定一个百分制的成绩，并通过"when"语句中的区间匹配将其转换为等第。
 */
fun example06() {
    val score = 85
    val grade = when {
        score >= 90 -> "优"
        score >= 75 -> "良"
        score >= 60 -> "中"
        else -> "差"
    }
    println("等第为：$grade")
}

/**
 * 示例七：判断输入值与区间的关系。
 *
 * 在本示例中，我们给定一个数字，判断它与区间 `(4, 6)` 的关系。
 */
fun example07() {
    val x = 5
    if (x > 4) {
        if (x > 6) {
            println("x大于6")
        } else {
            println("x在4到6之间")
        }
    } else {
        println("x小于等于4")
    }
}
