package net.bi4vmr.androidtoolkit.data.config.enums

/**
 * ADB 可执行文件选择模式。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ADBExecFileMode {

    /**
     * 程序内置。
     */
    INTERNAL,

    /**
     * 自动侦测。
     */
    AUTO,

    /**
     * 用户定义。
     */
    USER;
}