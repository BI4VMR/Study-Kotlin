package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * Android 系统版本。
 *
 * 已发布的版本信息，对于后续新增版本应手动添加。所有版本信息可参考 [AOSP 版本号](https://source.android.com/docs/setup/reference/build-numbers)。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class AndroidVersion(

    /**
     * 版本号。
     *
     * 例如：`8.1`、`16.1`、`17`。
     */
    val version: String,

    /**
     * API Level。
     *
     * 早期版本为整数，例如：Android 10 对应 `27`；自从 Android 16 开始可能带有子版本号，例如：`36.1`、`37.0`。
     */
    val apiLevel: String,

    /**
     * 标准名称。
     *
     * 例如：`Android 8.0`。
     */
    val standardName: String,

    /**
     * 版本代号。
     *
     * 例如：`Oreo`、`Pie`、`Tiramisu`。
     */
    val codeName: String
) {
    /**
     * 未知版本。
     */
    UNKNOWN("0", "0", "-", "Unknown"),

    /**
     * Android 1.0。
     */
    V_1_0("1.0", "1", "Android 1.0", "Base"),

    /**
     * Android 1.1。
     */
    V_1_1("1.1", "2", "Android 1.1", "Base 1.1"),

    /**
     * Android 1.5。
     */
    V_1_5("1.5", "3", "Android 1.5", "Cupcake"),

    /**
     * Android 1.6。
     */
    V_1_6("1.6", "4", "Android 1.6", "Donut"),

    /**
     * Android 2.0。
     */
    V_2_0("2.0", "5", "Android 2.0", "Eclair"),

    /**
     * Android 2.0.1。
     */
    V_2_0_1("2.0.1", "6", "Android 2.0.1", "Eclair"),

    /**
     * Android 2.1。
     */
    V_2_1("2.1", "7", "Android 2.1", "Eclair"),

    /**
     * Android 2.2。
     */
    V_2_2("2.2", "8", "Android 2.2", "Froyo"),

    /**
     * Android 2.3。
     */
    V_2_3("2.3", "9", "Android 2.3", "Gingerbread"),

    /**
     * Android 2.3.3。
     */
    V_2_3_3("2.3.3", "10", "Android 2.3.3", "Gingerbread"),

    /**
     * Android 3.0。
     */
    V_3_0("3.0", "11", "Android 3.0", "Honeycomb"),

    /**
     * Android 3.1。
     */
    V_3_1("3.1", "12", "Android 3.1", "Honeycomb"),

    /**
     * Android 3.2。
     */
    V_3_2("3.2", "13", "Android 3.2", "Honeycomb"),

    /**
     * Android 4.0。
     */
    V_4_0("4.0", "14", "Android 4.0", "Ice Cream Sandwich"),

    /**
     * Android 4.0.3。
     */
    V_4_0_3("4.0.3", "15", "Android 4.0.3", "Ice Cream Sandwich"),

    /**
     * Android 4.1。
     */
    V_4_1("4.1", "16", "Android 4.1", "Jelly Bean"),

    /**
     * Android 4.2。
     */
    V_4_2("4.2", "17", "Android 4.2", "Jelly Bean"),

    /**
     * Android 4.3。
     */
    V_4_3("4.3", "18", "Android 4.3", "Jelly Bean"),

    /**
     * Android 4.4。
     */
    V_4_4("4.4", "19", "Android 4.4", "KitKat"),

    /**
     * Android 4.4W。
     *
     * 面向穿戴设备的特殊版本。
     */
    V_4_4W("4.4W", "20", "Android 4.4W", "KitKat Wear"),

    /**
     * Android 5.0。
     */
    V_5_0("5.0", "21", "Android 5.0", "Lollipop"),

    /**
     * Android 5.1。
     */
    V_5_1("5.1", "22", "Android 5.1", "Lollipop"),

    /**
     * Android 6.0。
     */
    V_6_0("6.0", "23", "Android 6.0", "Marshmallow"),

    /**
     * Android 7.0。
     */
    V_7_0("7.0", "24", "Android 7.0", "Nougat"),

    /**
     * Android 7.1。
     */
    V_7_1("7.1", "25", "Android 7.1", "Nougat"),

    /**
     * Android 8.0。
     */
    V_8_0("8.0", "26", "Android 8.0", "Oreo"),

    /**
     * Android 8.1。
     */
    V_8_1("8.1.0", "27", "Android 8.1", "Oreo"),

    /**
     * Android 9。
     */
    V_9("9", "28", "Android 9", "Pie"),

    /**
     * Android 10。
     */
    V_10("10", "29", "Android 10", "Q"),

    /**
     * Android 11。
     */
    V_11("11", "30", "Android 11", "R"),

    /**
     * Android 12。
     */
    V_12("12", "31", "Android 12", "S"),

    /**
     * Android 12L。
     *
     * 面向大屏设备的特殊版本。
     */
    V_12L("12L", "32", "Android 12L", "S V2"),

    /**
     * Android 13。
     */
    V_13("13", "33", "Android 13", "Tiramisu"),

    /**
     * Android 14。
     */
    V_14("14", "34", "Android 14", "Upside Down Cake"),

    /**
     * Android 15。
     */
    V_15("15", "35", "Android 15", "Vanilla Ice Cream"),

    /**
     * Android 16。
     */
    V_16("16", "36", "Android 16", "Baklava"),

    /**
     * Android 16 演进版本一。
     */
    V_16_1("16.1", "36.1", "Android 16.1", "Baklava"),

    /**
     * Android 17。
     */
    V_17("17", "37.0", "Android 17", "Cinnamon Bun");

    companion object {

        /**
         * 根据版本号查找枚举。
         *
         * @param[input] Android 版本号。
         * @return 枚举。未匹配时将返回空值。
         */
        fun fromVersion(input: String): AndroidVersion = entries.firstOrNull { it.version == input } ?: UNKNOWN

        /**
         * 根据 API Level 查找枚举。
         *
         * @param[input] API Level。
         * @return 枚举。未匹配时将返回空值。
         */
        fun fromAPILevel(input: String): AndroidVersion = entries.firstOrNull { it.apiLevel == input } ?: UNKNOWN
    }

    /**
     * 获取完整 API Level。
     *
     * 自从 Android 16 开始，API Level 可能带有子版本号，例如：`36.1`、`37.0`。
     *
     * Android 16 提供了新的属性 `Build.VERSION.SDK_INT_FULL`，计算规则为 `major * 100000 + minor`，便于比较数值大小确定版本新旧。
     *
     * 旧版本没有子版本号，minor 始终为 `0`。
     *
     * @return 完整版本号。例如：`27` 返回 `2700000`，`36.1` 返回 `3600001`。
     */
    fun getAPILevelFull(): Int {
        val major = apiLevel.substringBefore('.').toInt()
        val minor = apiLevel.substringAfter('.', "0").toInt()
        return major * 100000 + minor
    }

    /**
     * 获取主要 API Level。
     *
     * 例如：`36.1` 返回 `36`。适用于仅关心主要版本号的场景。
     *
     * @return 主要 API Level。
     */
    fun getAPILevelMajor(): Int {
        return getAPILevelFull() / 100000
    }
}
