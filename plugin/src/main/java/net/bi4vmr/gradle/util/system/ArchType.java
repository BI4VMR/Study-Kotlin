package net.bi4vmr.gradle.util.system;

import net.bi4vmr.gradle.util.TextUtil;

/**
 * 平台架构类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public enum ArchType {

    /**
     * {@code x86}.
     */
    X86("x86", "i386", "i486", "i586", "i686"),

    /**
     * {@code x86_64}.
     */
    X86_64("x86_64", "amd64", "x64"),

    /**
     * {@code arm64}.
     */
    ARM64("arm64", "aarch64"),

    /**
     * 未知。
     */
    UNKNOWN("unknown");


    /**
     * 别名列表。
     * <p>
     * 当前架构可能具有的别名。
     */
    public final String[] aliases;

    // 构造方法
    ArchType(String... aliases) {
        this.aliases = aliases;
    }

    /**
     * 判断输入值是否能够匹配当前架构的任意别名。
     *
     * @param input 输入参数。
     * @return {@code true} 表示别名匹配成功； {@code false} 表示别名匹配失败。
     */
    public boolean isAliasMatch(String input) {
        if (TextUtil.isBlank(input)) {
            return false;
        }

        for (String alias : aliases) {
            if (input.toLowerCase().contains(alias.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
