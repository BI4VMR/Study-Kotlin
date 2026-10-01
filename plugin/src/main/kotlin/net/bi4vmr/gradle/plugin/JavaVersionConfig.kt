package net.bi4vmr.gradle.plugin

import org.gradle.api.JavaVersion

/**
 * Java SDK 版本插件配置项。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
open class JavaVersionConfig {

    companion object {
        const val NAME = "javaVersionConfig"
    }

    /**
     * JDK 版本号。
     *
     * 当前版本默认使用 JDK 21 (LTS) 。
     */
    var jdkVersion: JavaVersion = JavaVersion.VERSION_21
}
