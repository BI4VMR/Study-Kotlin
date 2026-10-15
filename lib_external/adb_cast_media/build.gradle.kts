import net.bi4vmr.gradle.util.LogUtil
import net.bi4vmr.gradle.util.system.OSType
import net.bi4vmr.gradle.util.system.SystemUtil

val depInTOML: MinimalExternalModuleDependency = privateLibKotlin.external.adb.cast.media.get()
val mvnGroupID: String = requireNotNull(depInTOML.group)
val mvnArtifactID: String = depInTOML.name
val mvnVersion: String = requireNotNull(depInTOML.version)


plugins {
    alias(libKotlin.plugins.core)

    alias(privateLibJava.plugins.java.version)
    alias(privateLibJava.plugins.publish.private)
}

tasks.withType<Test> {
    // 连接 Gradle 测试任务与 JUnit 工具
    useJUnitPlatform()
}

/* JavaCV 组件平台选择 */
val javacvPlatform: String = when (SystemUtil.getOSType()) {
    OSType.WINDOWS -> "windows-x86_64"
    OSType.LINUX -> "linux-x86_64"
    OSType.MACOS -> {
        val arch = System.getProperty("os.arch")
        if (arch == "aarch64" || arch == "arm64") "macosx-arm64" else "macosx-x86_64"
    }

    else -> throw IllegalStateException("Unsupported OS: ${SystemUtil.getOSType()}!")
}
LogUtil.info("JavaCV will use platform [$javacvPlatform].")

dependencies {
    /* 工程组件 */
    // 本地调试
    api(project(":lib_external:adb_cast_common"))

    // 依赖传递
    // api(privateLibKotlin.external.adb.cast.common)


    /* 外部组件 */
    api(libJava.slf4j.api)
    api(libJava.javacv.api)
    api("${libJava.javacv.ffmpeg.get()}:$javacvPlatform")


    /* 测试组件 */
    // JUnit5 BOM 版本配置文件
    testImplementation(platform(libJava.junit5.bom))
    // JUnit5 平台启动器
    testImplementation(libJava.junit5.launcher)
    // Jupiter（JUnit5 引擎的实现）
    testImplementation(libJava.junit5.jupiter)
}

javaVersionConfig {
    jdkVersion = JavaVersion.VERSION_1_8
}

privatePublishConfig {
    groupID = mvnGroupID
    artifactID = mvnArtifactID
    version = mvnVersion
}
