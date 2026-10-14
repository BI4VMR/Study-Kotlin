package net.bi4vmr.gradle.util.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 设备与系统工具。
 * <p>
 * 获取硬件平台、操作系统、JVM等环境信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class SystemUtil {

    private static final Logger logger = LoggerFactory.getLogger(SystemUtil.class);

    /**
     * 环境变量名称：PATH。
     * <p>
     * 多平台通用变量：可执行文件搜索路径。
     */
    public static final String ENV_NAME_PATH = "PATH";

    /**
     * 系统属性：操作系统名称。
     */
    private static final String PROPERTY_OS_NAME = System.getProperty("os.name");

    /**
     * 系统属性：平台架构名称。
     */
    private static final String PROPERTY_OS_ARCH = System.getProperty("os.arch");

    /**
     * 系统属性：操作系统版本。
     */
    private static final String PROPERTY_OS_VERSION = System.getProperty("os.version");

    /**
     * 当前操作系统类型。
     * <p>
     * 进程启动后操作系统类型不会改变，因此仅需在类加载时获取一次该值即可。
     */
    private static final OSType OS_TYPE = getOSTypeInner();

    /**
     * 当前平台架构名称。
     * <p>
     * 进程启动后操作系统类型不会改变，因此仅需在类加载时获取一次该值即可。
     */
    private static final ArchType OS_ARCH = getArchTypeInner();


    /*
     * ----- 系统信息 -----
     */

    /**
     * 获取当前操作系统类型。
     * <p>
     * 类加载时通过 {@link #getOSTypeInner()} 方法解析当前系统类型，后续返回缓存变量的值。
     *
     * @return {@link OSType} 枚举常量。
     */
    public static OSType getOSType() {
        return OS_TYPE;
    }

    /**
     * 获取当前平台架构类型。
     * <p>
     * 类加载时通过 {@link #getArchTypeInner()} 方法解析当前平台架构类型，后续返回缓存变量的值。
     *
     * @return {@link ArchType} 枚举常量。
     */
    public static ArchType getArchType() {
        return OS_ARCH;
    }

    /**
     * 判断当前操作系统是否为Windows。
     *
     * @return {@code true} 表示操作系统为Windows； {@code false} 表示其他系统。
     */
    public static boolean isWindows() {
        return getOSType() == OSType.WINDOWS;
    }

    /**
     * 判断当前操作系统是否为Linux。
     *
     * @return {@code true} 表示操作系统为Linux； {@code false} 表示其他系统。
     */
    public static boolean isLinux() {
        return getOSType() == OSType.LINUX;
    }

    /**
     * 判断当前操作系统是否为MacOS。
     *
     * @return {@code true} 表示操作系统为MacOS； {@code false} 表示其他系统。
     */
    public static boolean isMacOS() {
        return getOSType() == OSType.MACOS;
    }

    /**
     * 获取当前操作系统类型。
     * <p>
     * 将 {@code os.name} 系统属性与已知系统名称列表进行匹配，判断当前运行环境。
     *
     * @return {@link OSType} 枚举常量。
     */
    private static OSType getOSTypeInner() {
        for (OSType type : OSType.values()) {
            if (type.isAliasMatch(PROPERTY_OS_NAME)) {
                return type;
            }
        }

        return OSType.UNKNOWN;
    }

    /**
     * 获取当前平台架构名称。
     * <p>
     * 将 {@code os.arch} 系统属性与已知平台架构列表进行匹配，判断当前运行环境。
     *
     * @return {@link ArchType} 枚举常量。
     */
    private static ArchType getArchTypeInner() {
        for (ArchType type : ArchType.values()) {
            if (type.isAliasMatch(PROPERTY_OS_ARCH)) {
                return type;
            }
        }

        return ArchType.UNKNOWN;
    }


    /*
     * ----- 环境变量 -----
     */

    /**
     * 获取环境变量 {@code PATH} 的值。
     *
     * @return 变量值。非空，罕见的情况下将返回空字符串。
     */
    public static String getPath() {
        String rawValue = System.getenv(ENV_NAME_PATH);
        if (rawValue == null) {
            logger.error("Environment variable [PATH] is not exist!");
            return "";
        }
        return rawValue;
    }

    /**
     * 获取环境变量 {@code PATH} 的目录数组。
     * <p>
     * 根据平台解析变量值，每个数组元素表示一个目录。
     *
     * @return 目录数组。
     */
    public static String[] getPathDirectories() {
        return getPath().split(File.pathSeparator);
    }
}
