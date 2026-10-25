package net.bi4vmr.androidtoolkit.data.config.enums

/**
 * ADB 可执行文件选择模式。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ADBExecFileMode(

    /**
     * 配置编码。
     */
    val code: Int
) {

    /**
     * 程序内置。
     */
    INTERNAL(1),

    /**
     * 自动侦测。
     */
    AUTO(2),

    /**
     * 用户定义。
     */
    USER(3);

    companion object {

        fun fromCode(code: Int): ADBExecFileMode {
            return entries.firstOrNull { it.code == code } ?: INTERNAL
        }
    }
}
