package net.bi4vmr.androidtoolkit.base.enums

/**
 * 主题。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class Theme(

    /**
     * 编码。
     */
    val code: Int
) {

    /**
     * 纯色。
     */
    AUTO(1);


    companion object {

        /**
         * 根据编码获取枚举。
         *
         * @param[input] 编码。
         * @return 枚举，若无匹配项则返回 [AUTO] 。
         */
        fun fromCode(input: Int): Theme {
            return entries.firstOrNull { it.code == input } ?: AUTO
        }
    }
}
