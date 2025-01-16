package com.android.systemui.car.vendor.tbox

/**
 * TBox数据网络信号强度(WCDMA-RSCP)。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class CarTBoxWCDMARSCPLevel(

    /**
     * 当前级别对应的最大RSCP数值（不包含）。
     */
    val maxValue: Int
) {

    /**
     * RSCP：(-∞, -111)
     */
    LEVEL_0(-111),

    /**
     * RSCP：[-111, -105)
     */
    LEVEL_1(-105),

    /**
     * RSCP：[-105, -99)
     */
    LEVEL_2(-99),

    /**
     * RSCP：[-99, -89)
     */
    LEVEL_3(-89),

    /**
     * RSCP：[-89, -79)
     */
    LEVEL_4(-79),

    /**
     * RSCP：[-79, 0)
     */
    LEVEL_5(0);

    companion object {

        /**
         * 将RSCP数值转换为信号强度级别。
         *
         * @param[value] RSCP数值。
         * @return 信号级别。
         */
        fun parseFromRSCP(value: Int): CarTBoxWCDMARSCPLevel {
            return when {
                value >= LEVEL_0.maxValue && value < LEVEL_1.maxValue -> LEVEL_1
                value >= LEVEL_1.maxValue && value < LEVEL_2.maxValue -> LEVEL_2
                value >= LEVEL_2.maxValue && value < LEVEL_3.maxValue -> LEVEL_3
                value >= LEVEL_3.maxValue && value < LEVEL_4.maxValue -> LEVEL_4
                value >= LEVEL_4.maxValue && value < LEVEL_5.maxValue -> LEVEL_5
                else -> LEVEL_0
            }
        }
    }
}
