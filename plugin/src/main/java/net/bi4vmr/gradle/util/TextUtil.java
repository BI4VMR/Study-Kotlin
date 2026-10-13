package net.bi4vmr.gradle.util;

/**
 * 文本相关工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class TextUtil {

    /**
     * 判断字符串内容是否为空。
     * <p>
     * 字符串对象为空或其内容为空，都视为空字符串。
     *
     * @param s 待测试的字符串。
     * @return {@code true} 表示字符串为空； {@code false} 表示字符串非空。
     */
    public static boolean isEmpty(String s) {
        return (s == null) || (s.isEmpty());
    }

    /**
     * 判断字符串内容是否非空。
     * <p>
     * 字符串对象为空或其内容为空，都视为空字符串。
     *
     * @param s 待测试的字符串。
     * @return {@code true} 表示字符串为空； {@code false} 表示字符串非空。
     */
    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    /**
     * 判断字符串内容是否为空或空格。
     *
     * @param s 待测试的字符串。
     * @return {@code true} 表示字符串为空或空格； {@code false} 表示字符串非空且不只包含空格。
     */
    public static boolean isBlank(String s) {
        return (s == null) || (s.trim().isEmpty());
    }

    /**
     * 判断字符串内容是否非空且不只包含空格。
     *
     * @param s 待测试的字符串。
     * @return {@code true} 表示字符串非空且不只包含空格； {@code false} 表示字符串为空或空格。
     */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }
}
