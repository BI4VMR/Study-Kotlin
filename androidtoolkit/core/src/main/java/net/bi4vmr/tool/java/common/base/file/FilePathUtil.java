package net.bi4vmr.tool.java.common.base.file;

import net.bi4vmr.tool.java.common.base.general.TextUtil;

/**
 * 文件路径工具。
 * <p>
 * 本工具旨在处理远程路径或尚未创建的文件路径，对于已有文件请使用 {@link java.io.File} 等工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class FilePathUtil {

    private FilePathUtil() {
        // 静态工具类，无需实例化。
    }

    /**
     * 获取路径类型。
     *
     * @param path 路径。
     * @return 路径类型枚举。未命中常见规则的路径和空路径均被视为相对路径。
     */
    public static FilePathType getType(String path) {
        // 空路径
        if (TextUtil.isBlank(path)) {
            return FilePathType.RELATIVE;
        }

        if (path.startsWith("/")) {
            return FilePathType.UNIX;
        }

        if (path.length() > 2 &&
                path.startsWith("\\\\") &&
                path.charAt(2) != '\\'
        ) {
            return FilePathType.UNC;
        }

        if (path.length() > 2 &&
                Character.isLetter(path.charAt(0)) &&
                path.charAt(1) == ':' &&
                path.charAt(2) == '\\'
        ) {
            return FilePathType.WINDOWS;
        }

        // 其他格式，视为相对路径。
        return FilePathType.RELATIVE;
    }

    /**
     * 判断路径是否为根目录。
     * <p>
     * 空路径和相对路径表示当前目录，无法确定是否为根目录，因此固定返回 {@code false}。
     *
     * @param path 路径。
     * @return {@code true} 表示根路径，{@code false} 表示子路径。
     */
    public static boolean isRoot(String path) {
        if (TextUtil.isBlank(path)) {
            return false;
        }

        FilePathType type = getType(path);
        switch (type) {
            case UNIX:
                return path.equals("/");
            case WINDOWS:
                return path.length() == 3;
            case UNC:
                // UNC 根路径的格式为 "\\server"，因此根据反斜杠拆分后元素数量必为 3，如果路径后有其他节点数组元素至少为 4 个。
                return path.split("\\\\").length == 3;
            default:
                return false;
        }
    }

    /**
     * 判断路径是否指向目录。
     * <p>
     * 按照惯例，{@code /} 或 {@code \} 结尾的路径将被视为目录，其他情况将被视为文件。
     *
     * @param path 路径。
     * @return {@code true} 表示目录，{@code false} 表示文件或无法判断。
     */
    public static boolean isDirectory(String path) {
        // 空路径表示当前目录
        if (TextUtil.isBlank(path)) {
            return true;
        }

        return isSeparator(path.charAt(path.length() - 1));
    }

    /**
     * 获取父目录。
     *
     * @param path 路径。
     * @return 父目录路径。如果是根目录，则为空值；如果是相对路径，则返回 {@code "."}。
     */
    public static String getParent(String path) {
        // 对于相对路径，无法获取上层目录信息，至多返回指代“当前目录”的点。
        if (TextUtil.isBlank(path) || path.equals(".") || path.equals("./") || path.equals(".\\")) {
            return ".";
        }

        // 如果是根目录，则返回空值。
        if (isRoot(path)) {
            return null;
        }

        path = normalize(path);
        String pathNoRoot = removeRoot(path);
        String root = getRoot(path);
        int index = findLastSeparator(pathNoRoot);
        if (index < 0) {
            // 去除根目录后剩余路径没有分隔符，说明是一个根目录下的文件或目录，例如：`/test.txt`，返回根目录。
            return root;
        } else {
            // 已找到分隔符，则返回分隔符之前的内容。
            return path.substring(0, index + root.length());
        }
    }


    /**
     * 获取文件名称。
     * <p>
     * 如果路径指向目录，则返回目录名称。
     *
     * @param path 路径。
     * @return 文件名称。如果是根目录，则为空值。
     */
    public static String getFileName(String path) {
        // 如果是根目录，则返回空值。
        if (isRoot(path)) {
            return null;
        }

        path = normalize(path);
        String pathNoRoot = removeRoot(path);
        int index = findLastSeparator(pathNoRoot);
        if (index < 0) {
            // 去除根目录后剩余路径没有分隔符，说明是一个根目录下的文件或目录，例如：`/test.txt`。
            return path;
        } else {
            // 已找到分隔符，则返回分隔符之前的内容。
            return path.substring(0, index + root.length());
        }
        int start = hasDrivePrefix(normalized) ? 2 : 0;
        int sepIndex = lastSeparatorIndex(normalized);
        if (sepIndex >= start) {
            start = sepIndex + 1;
        }
        return normalized.substring(start);
    }

    /**
     * 获取扩展名。
     *
     * @param path 路径。
     * @return 扩展名。目录、隐藏文件等返回空字符串。
     */
    public static String getExtension(String path) {
        // // 目录没有扩展名。
        // if (isDirectory(path)) {
        //     return "";
        // }
        //
        // String fileName = getFileName(path);
        // int dotIndex = fileName.lastIndexOf('.');
        // // 无点、点在首位（隐藏文件）或点在末尾，均视为无扩展名。
        // if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
        //     return "";
        // }
        // return fileName.substring(dotIndex + 1);
        return "";
    }

    /**
     * 获取无扩展名的文件名。
     *
     * @param path 路径。
     * @return 文件名。
     */
    public static String getFileNameNoExtension(String path) {
        return "";
    }


    /*
     * ----- 内部方法 -----
     */

    /**
     * 判断字符是否为路径分隔符。
     *
     * @param c 字符。
     * @return {@code true} 表示分隔符，{@code false} 表示非分隔符。
     */
    private static boolean isSeparator(char c) {
        return c == '/' || c == '\\';
    }

    /**
     * 格式化路径。
     * <p>
     * 移除可能存在的转义符号，如果路径指向目录，移除末尾的斜杠。
     * <p>
     * 仅支持通用语义，例如：Windows 不支持反斜杠紧接空格，如果出现这种情况必然是一个 Unix 路径，移除转义符号不会干扰输入为 Windows 路径的场景。
     * 对于 Unix 的 {@code \(} 等则无法移除，因为这在 Windows 中是允许的，如果输入为相对路径则无法区分平台，盲目移除转义符号会破坏路径。
     *
     * @param path 路径。
     * @return 处理后的路径。
     */
    private static String normalize(String path) {
        // 去掉 Unix 转义空格
        path = path.replace("\\ ", " ");
        // 去掉 Unix 转义问号
        path = path.replace("\\?", "?");
        // 去掉 Unix 转义分隔符
        path = path.replace("\\|", "|");

        // 如果指向目录，且不是根目录或空字符串，去掉末尾的分隔符。
        if (isDirectory(path) && TextUtil.isNotBlank(path) && !isRoot(path)) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * 获取根目录。
     *
     * @param path 路径。
     * @return 根目录。相对路径返回 {@code "."}。
     */
    private static String getRoot(String path) {
        FilePathType type = getType(path);
        switch (type) {
            case UNIX:
                return "/";
            case WINDOWS:
                return path.substring(0, 3);
            case UNC:
                int index = path.indexOf("\\", 2);
                if (index < 0) {
                    return path;
                }
                return path.substring(0, index + 1);
            default:
                return ".";
        }
    }

    /**
     * 移除根目录。
     *
     * @param path 路径。
     * @return 处理后的路径。可能是空字符串，但不为空值。
     */
    private static String removeRoot(String path) {
        switch (getType(path)) {
            case UNIX:
                return path.substring(1);
            case WINDOWS:
                return path.substring(3);
            case UNC:
                // UNC 根路径可能是 `\\<主机>` 或 `\\<主机>\`，截取主机之后的部分。
                int index = path.indexOf("\\", 2);
                // 如果是 `\\<主机>`，表示根路径，返回空字符串。
                if (index < 0) {
                    return "";
                }
                return path.substring(index + 1);
            default:
                // 相对路径不作处理。
                return path;
        }
    }

    /**
     * 寻找末端分隔符。
     *
     * @return 下标。未找到时返回 {@code -1}。
     */
    private static int findLastSeparator(String path) {
        if (TextUtil.isBlank(path)) {
            return -1;
        }

        for (int i = path.length() - 1; i >= 0; i--) {
            if (isSeparator(path.charAt(i))) {
                return i;
            }
        }

        return -1;
    }
}
