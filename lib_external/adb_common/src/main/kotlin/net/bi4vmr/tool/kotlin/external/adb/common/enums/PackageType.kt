package net.bi4vmr.tool.kotlin.external.adb.common.enums

/**
 * 软件包类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class PackageType(

    /**
     * ADB 命令行参数。
     */
    val cmdArgs: String
) {

    /**
     * 所有。
     */
    ALL(""),

    /**
     * 系统应用。
     */
    SYSTEM("-s"),

    /**
     * 第三方。
     */
    THREERD_PARTY("-3"),

    /**
     * 已禁用。
     */
    DISABLED("-d"),

    /**
     * 已启用。
     */
    ENABLED("-e");
}
