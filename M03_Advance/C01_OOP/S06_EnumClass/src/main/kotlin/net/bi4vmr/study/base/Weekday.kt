package net.bi4vmr.study.base;

/**
 * 枚举类：星期。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
enum class Weekday(
    /* 构造方法 */
    // 定义属性：索引编号
    private val index: Int,
    // 定义属性：标准名称
    private val standardName: String
) {

    MONDAY(1, "周一"),
    TUESDAY(2, "周二"),
    WEDNESDAY(3, "周三"),
    THURSDAY(4, "周四"),
    FRIDAY(5, "周五"),
    SATURDAY(6, "周六"),
    SUNDAY(7, "周日");

    companion object {

        /**
         * 根据序号获取枚举常量。
         *
         * 未匹配到对应的常量时，将返回空值。
         *
         * @param[ordinal] 枚举列表中的序号。
         * @return 枚举常量。
         */
        @JvmStatic
        fun parseFromSerial(ordinal: Int): Weekday? {
            // 遍历所有常量
            values().forEach {
                // 如果某个常量的序号与传入参数相同，则返回该常量并终止循环。
                if (it.ordinal == ordinal) {
                    return it
                }
            }

            // 如果传入参数未匹配到任何常量，则返回空值。
            return null
        }
    }

    // 获取索引编号
    fun getIndex(): Int {
        return index
    }

    // 获取标准名称
    fun getStandardName(): String {
        return standardName
    }

    /**
     * 获取上一项。
     *
     * @return 枚举常量。
     */
    fun previous(): Weekday {
        val items: Array<Weekday> = values()
        return if (ordinal == 0) {
            // 当前常量为第一项时，返回最后一项。
            items[items.lastIndex]
        } else {
            // 当前常量不是第一项时，返回前一项。
            items[ordinal - 1]
        }
    }

    /**
     * 获取下一项。
     *
     * @return 枚举常量。
     */
    fun next(): Weekday {
        val items: Array<Weekday> = values()
        return if (ordinal < items.lastIndex) {
            // 当前常量的序号小于最后一项的序号时，返回后一项。
            items[ordinal + 1]
        } else {
            // 当前常量的序号等于最后一项的序号时，返回第一项。
            items[0]
        }
    }
}
