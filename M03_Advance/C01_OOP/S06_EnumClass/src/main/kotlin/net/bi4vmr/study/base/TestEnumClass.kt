package net.bi4vmr.study.base

/**
 * 测试类：枚举类。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
fun main() {
    example03()
}

/**
 * 示例一：使用常量表示季节。
 *
 * 在本示例中，我们使用数字代码表示四个季节。
 */
fun example01() {
    printSeason(Const.SEASON_SPRING)
}

// 根据传入的季节代号，输出季节名称。
fun printSeason(code: Int) {
    when (code) {
        Const.SEASON_SPRING -> println("现在是春天")
        Const.SEASON_SUMMER -> println("现在是夏天")
        Const.SEASON_AUTUMN -> println("现在是秋天")
        Const.SEASON_WINTER -> println("现在是冬天")
        else -> println("无效的参数！")
    }
}

/**
 * 示例二：使用枚举表示季节。
 *
 * 在本示例中，我们使用枚举表示四个季节。
 */
fun example02() {
    printSeason2(Season.SUMMER)
}

// 根据传入的枚举常量，输出季节名称。
fun printSeason2(season: Season) {
    when (season) {
        Season.SPRING -> println("现在是春天")
        Season.SUMMER -> println("现在是夏天")
        Season.AUTUMN -> println("现在是秋天")
        Season.WINTER -> println("现在是冬天")
    }
}

/**
 * 示例三：访问枚举类的内置属性。
 *
 * 在本示例中，我们访问Season中枚举的序号与名称属性，并将它们输出到控制台上。
 */
fun example03() {
    // 获取常量的序号
    println("访问春天的序号：${Season.SPRING.ordinal}")
    // 获取常量的名称
    println("访问秋天的名称：${Season.AUTUMN.name}")
}

/**
 * 示例四：为枚举类新增自定义属性与方法。
 *
 * 在本示例中，我们使用枚举表示一周中的七天，并添加一些自定义属性与方法。
 */
fun example04() {
    // 调用自定义方法
    println("${Weekday.SUNDAY.getStandardName()}的序号是：${Weekday.SUNDAY.getIndex()}")
}

/**
 * 示例五：常用方法模板。
 */
fun example05() {
    // 根据序号获取对应的枚举常量
    println("根据序号获取对应的枚举常量：${Weekday.parseFromSerial(0)}")

    // 获取上一项
    println()
    println("获取周一的上一项：${Weekday.MONDAY.previous().getStandardName()}")
    println("获取周日的上一项：${Weekday.SUNDAY.previous().getStandardName()}")

    // 获取下一项
    println()
    println("获取周一的下一项：${Weekday.MONDAY.next().getStandardName()}")
    println("获取周日的下一项：${Weekday.SUNDAY.next().getStandardName()}")
}
