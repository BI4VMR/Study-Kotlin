package com.android.systemui.car.vendor.tbox

/**
 * TBox数据网络信号强度(WCDMA-Ec/No)。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class CarTBoxWCDMAEcNoLevel(

    /**
     * 当前级别对应的最大Ec/No数值（不包含）。
     */
    val maxValue: Int
) {

    /**
     * SNR：(-∞, -18)
     */
    LEVEL_0(-18),

    /**
     * SNR：[-18, -16)
     */
    LEVEL_1(-16),

    /**
     * SNR：[-16, -14)
     */
    LEVEL_2(-14),

    /**
     * SNR：[-14, -12)
     */
    LEVEL_3(-12),

    /**
     * SNR：[-12, -10)
     */
    LEVEL_4(-10),

    /**
     * SNR：[-10, +∞)
     */
    LEVEL_5(0);

    companion object {

        /**
         * 将Ec/No数值转换为信号强度级别。
         *
         * @param[value] Ec/No数值。
         * @return 信号级别。
         */
        fun parseFromSNR(value: Int): CarTBoxWCDMAEcNoLevel {
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
