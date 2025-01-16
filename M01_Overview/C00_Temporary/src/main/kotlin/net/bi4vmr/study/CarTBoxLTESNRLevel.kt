package com.android.systemui.car.vendor.tbox

/**
 * TBox数据网络信号强度(LTE-SNR)。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class CarTBoxLTESNRLevel(

    /**
     * 当前级别对应的最大SNR数值（不包含）。
     */
    val maxValue: Float
) {

    /**
     * SNR：(-∞, -6.0)
     */
    LEVEL_0(-6.0F),

    /**
     * SNR：[-6.0, -3.0)
     */
    LEVEL_1(-3.0F),

    /**
     * SNR：[-3.0, 1.0)
     */
    LEVEL_2(1.0F),

    /**
     * SNR：[1.0, 4.5)
     */
    LEVEL_3(4.5F),

    /**
     * SNR：[4.5, 13)
     */
    LEVEL_4(13.0F),

    /**
     * SNR：[13, +∞)
     */
    LEVEL_5(Float.MAX_VALUE);

    companion object {

        /**
         * 将SNR数值转换为信号强度级别。
         *
         * @param[value] SNR数值。
         * @return 信号级别。
         */
        fun parseFromSNR(value: Int): CarTBoxLTESNRLevel {
            return when {
                value >= LEVEL_0.maxValue && value < LEVEL_1.maxValue -> LEVEL_1
                value >= LEVEL_1.maxValue && value < LEVEL_2.maxValue -> LEVEL_2
                value >= LEVEL_2.maxValue && value < LEVEL_3.maxValue -> LEVEL_3
                value >= LEVEL_3.maxValue && value < LEVEL_4.maxValue -> LEVEL_4
                value >= LEVEL_4.maxValue && value <= LEVEL_5.maxValue -> LEVEL_5
                else -> LEVEL_0
            }
        }
    }
}
