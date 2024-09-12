package net.bi4vmr.study.base

/**
 * 测试类：枚举类。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
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

// 根据传入的季节代号，输出季节名称。
fun printSeason2(season: Season) {
    when (season) {
        Season.SPRING -> println("现在是春天")
        Season.SUMMER -> println("现在是夏天")
        Season.AUTUMN -> println("现在是秋天")
        Season.WINTER -> println("现在是冬天")
    }
}
