package com.android.systemui.car.vendor.tbox

/**
 * TBox数据网络信号强度(LTE-RSRP)。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class CarTBoxLTERSRPLevel(

    /**
     * 当前级别对应的最大RSRP数值（不包含）。
     */
    val maxValue: Int
) {

    /**
     * RSRP：(-∞, -125)
     */
    LEVEL_0(-125),

    /**
     * RSRP：[-125, -120)
     */
    LEVEL_1(-120),

    /**
     * RSRP：[-120, -115)
     */
    LEVEL_2(-115),

    /**
     * RSRP：[-115, -109)
     */
    LEVEL_3(-109),

    /**
     * RSRP：[-109, -103)
     */
    LEVEL_4(-103),

    /**
     * RSRP：[-103, 0)
     */
    LEVEL_5(0);

    companion object {

        /**
         * 将RSRP数值转换为信号强度级别。
         *
         * @param[value] RSRP数值。
         * @return 信号级别。
         */
        fun parseFromRSRP(value: Int): CarTBoxLTERSRPLevel {
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
