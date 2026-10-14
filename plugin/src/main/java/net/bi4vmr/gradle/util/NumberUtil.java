package net.bi4vmr.gradle.util;

/**
 * 数字相关工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class NumberUtil {

    /*
     * ----- 数字与文本转换 -----
     */

    /**
     * 判断文本内容是否为整数。
     * <p>
     * 默认不支持正负号，若需要支持这种格式，请使用 {@link NumberUtil#isInteger(String, boolean)} 方法。
     *
     * @param text 待测试的文本。
     * @return `true` 表示文本内容是整数， `false` 表示文本内容不是整数。
     */
    public static boolean isInteger(String text) {
        return isInteger(text, false);
    }

    /**
     * 判断文本内容是否为整数。
     *
     * @param text        待测试的文本。
     * @param supportSign 是否支持符号，设为 `true` 时可以识别带有正负号的数字；设为 `false` 时，传入带有正负号的文本则被认为不是
     *                    数字。
     * @return `true` 表示文本内容是整数， `false` 表示文本内容不是整数。
     */
    public static boolean isInteger(String text, boolean supportSign) {
        if (TextUtil.isEmpty(text)) {
            return false;
        }

        // 如果不支持符号，且字符串以"+"或"-"开头，则认为该字符串不是整数。
        if (!supportSign && (text.charAt(0) == '+' || text.charAt(0) == '-')) {
            return false;
        }

        // 如果能够转为Integer类型，则认为该字符串是整数。
        try {
            Integer.parseInt(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将文本转为整型数值。
     *
     * @param text 输入文本。
     * @return 整型数值。
     * @throws NumberFormatException 文本无法被转为数字时将抛出该异常。
     */
    public static int toIntegerUnsafe(String text) {
        return Integer.parseInt(text);
    }

    /**
     * 将文本转为整型数值。
     * <p>
     * 文本无法被转为数字时，默认返回 `0` 。
     *
     * @param text 输入文本。
     * @return 整型数值。
     */
    public static int toInteger(String text) {
        try {
            return toIntegerUnsafe(text);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 将文本转为整型数值。
     * <p>
     * 文本无法被转为数字时，返回第二参数指定的默认值。
     *
     * @param text     输入文本。
     * @param defValue 默认值。
     * @return 整型数值。
     */
    public static int toInteger(String text, int defValue) {
        try {
            return toIntegerUnsafe(text);
        } catch (Exception e) {
            return defValue;
        }
    }

    /**
     * 判断文本内容是否为小数。
     * <p>
     * 整数也属于小数的一种。
     * <p>
     * 默认不支持正负号，若需要支持这种格式，请使用 {@link NumberUtil#isDecimal(String, boolean)} 方法。
     *
     * @param text 待测试的文本。
     * @return `true` 表示文本内容是小数， `false` 表示文本内容不是小数。
     */
    public static boolean isDecimal(String text) {
        return isDecimal(text, false);
    }

    /**
     * 判断文本内容是否为小数。
     * <p>
     * 整数也属于小数的一种。
     *
     * @param text        待测试的文本。
     * @param supportSign 是否支持符号，设为 `true` 时可以识别带有正负号的数字；设为 `false` 时，传入带有正负号的文本则被认为不是
     *                    数字。
     * @return `true` 表示文本内容是小数， `false` 表示文本内容不是小数。
     */
    public static boolean isDecimal(String text, boolean supportSign) {
        if (TextUtil.isEmpty(text)) {
            return false;
        }

        // 如果不支持符号，且字符串以"+"或"-"开头，则认为该字符串不是小数。
        if (!supportSign && (text.charAt(0) == '+' || text.charAt(0) == '-')) {
            return false;
        }

        // 如果能够转为Double类型，则认为该字符串是小数。
        try {
            Double.parseDouble(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将文本转为小数数值。
     *
     * @param text 输入文本。
     * @return 小数数值。
     * @throws NullPointerException  输入参数为空值时将抛出该异常。
     * @throws NumberFormatException 文本无法被转为数字时将抛出该异常。
     */
    public static double toDecimalUnsafe(String text) {
        return Double.parseDouble(text);
    }

    /**
     * 将文本转为小数数值。
     * <p>
     * 文本无法被转为数字时，默认返回 `0` 。
     *
     * @param text 输入文本。
     * @return 小数数值。
     */
    public static double toDecimal(String text) {
        try {
            return toDecimalUnsafe(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 将文本转为小数数值。
     * <p>
     * 文本无法被转为数字时，返回第二参数指定的默认值。
     *
     * @param text     输入文本。
     * @param defValue 默认值。
     * @return 小数数值。
     */
    public static double toDecimal(String text, double defValue) {
        try {
            return toDecimalUnsafe(text);
        } catch (Exception e) {
            return defValue;
        }
    }


    /*
     * ----- 进制转换 -----
     */

    /**
     * 将数字转为十六进制文本。
     * <p>
     * 将数字转为十六进制文本，不含 `0x` 前缀。
     *
     * @param data        待转换的数字。
     * @param needPadding 结果为单个字符时，是否在前面补 `0` 。
     * @param isUpperCase 是否将结果转换为大写字母。
     * @return 十六进制文本。永不为空值。
     */
    public static String toHexString(byte data, boolean needPadding, boolean isUpperCase) {
        /*
         * 单个字节的取值范围是：[0, 255]，当它被Java读取为 `byte` 类型时，大于 `127` 的值将被映射到负数，因此我们需要将 `byte` 值和
         * `0xFF` 做与运算，丢弃符号位以获取原始数值。
         */
        String hex = Integer.toHexString(data & 0xFF);
        // 如果该字节对应的16进制文本只有一个字符，则在前面补 `0` 。
        if (needPadding && hex.length() < 2) {
            hex = "0" + hex;
        }

        if (isUpperCase) {
            return hex.toUpperCase();
        } else {
            return hex;
        }
    }

    /**
     * 将数字转为十六进制文本。
     * <p>
     * 简化方法，结果为单个字符时，自动在前面补 `0` ，且英文字母均为大写。其他行为详见 {@link #toHexString(byte, boolean, boolean)} 方法。
     *
     * @param data 待转换的数字。
     * @return 十六进制文本。永不为空值。
     */
    public static String toHexString(byte data) {
        return toHexString(data, true, true);
    }

    /**
     * 将字节数组转为十六进制文本。
     *
     * @param datas       待转换的数组。
     * @param needPadding 元素为单个字符时，是否在前面补"0"。
     * @param isUpperCase 是否将结果转换为大写字母。
     * @return 十六进制文本。数组为空值时返回空值；数组没有内容时返回空字符串。
     */
    public static String toHexString(byte[] datas, boolean needPadding, boolean isUpperCase) {
        if (datas == null) {
            return null;
        }
        if (datas.length == 0) {
            return "";
        }


        StringBuilder buffer = new StringBuilder();
        for (byte b : datas) {
            String hex = toHexString(b, needPadding, isUpperCase);
            buffer.append(hex);
        }

        return buffer.toString();
    }

    /**
     * 将字节数组转为十六进制文本。
     * <p>
     * 简化方法，英文字母均为大写；若每个元素转换后为单个字符，自动在前面补 `0` 。其他行为详见 {@link #toHexString(byte[], boolean, boolean)} 方法。
     *
     * @param datas 待转换的数组。
     * @return 十六进制文本。数组为空值时返回空值；数组没有内容时返回空字符串。
     */
    public static String toHexString(byte[] datas) {
        return toHexString(datas, true, true);
    }


    /*
     * ----- 数字序号处理 -----
     */

    /**
     * 数字转为序号。
     * <p>
     * 在数字前添加 `0` ，生成序号。
     *
     * @param num    原始数值，必须大于0。
     * @param length 序号的位数。
     * @return 序号。参数不合法时将返回空值。
     */
    public static String toSerialNumber(int num, int length) {
        if (num <= 0 || length <= 0) {
            return null;
        }

        return String.format("%0" + length + "d", num);
    }
}
