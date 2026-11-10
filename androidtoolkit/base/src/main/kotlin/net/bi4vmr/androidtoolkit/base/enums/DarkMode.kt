package net.bi4vmr.androidtoolkit.data.config.enums

/**
 * 深色模式。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class DarkMode(

    /**
     * 编码。
     */
    val code: Int
) {

    /**
     * 跟随系统设置。
     */
    AUTO(1),

    /**
     * 浅色。
     */
    LIGHT(2),

    /**
     * 深色。
     */
    DARK(3);


    companion object {

        /**
         * 根据编码获取枚举。
         *
         * @param[code] 编码。
         * @return 枚举，若无匹配项则返回 [AUTO] 。
         */
        fun fromCode(input: Int): DarkMode {
            return entries.firstOrNull { it.code == input } ?: AUTO
        }
    }
}
