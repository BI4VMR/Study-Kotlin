package net.bi4vmr.tool.java.common.base.system;

/**
 * 操作系统类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public enum OSType {

    /**
     * Microsoft Windows.
     */
    WINDOWS("windows"),

    /**
     * GNU/Linux.
     */
    LINUX("linux"),

    /**
     * Apple MacOS.
     */
    MACOS("macos", "mac os"),

    /**
     * 未知。
     */
    UNKNOWN("unknown");


    /**
     * 别名列表。
     * <p>
     * 当前系统可能具有的别名，以小写字母表示。
     */
    public final String[] aliases;

    // 构造方法
    OSType(String... aliases) {
        this.aliases = aliases;
    }

    /**
     * 判断输入值是否能够匹配当前系统的任意别名。
     *
     * @param input 输入参数。
     * @return {@code true} 表示别名匹配成功； {@code false} 表示别名匹配失败。
     */
    public boolean isAliasMatch(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        for (String alias : aliases) {
            if (input.toLowerCase().contains(alias)) {
                return true;
            }
        }

        return false;
    }
}
