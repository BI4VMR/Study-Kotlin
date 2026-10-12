val depInTOML: MinimalExternalModuleDependency = privateLibKotlin.external.adb.core.get()
val mvnGroupID: String = requireNotNull(depInTOML.group)
val mvnArtifactID: String = depInTOML.name
val mvnVersion: String = requireNotNull(depInTOML.version)


plugins {
    // Kotlin JVM 平台插件
    alias(libKotlin.plugins.core)
    // Compose Multiplatform 插件
    alias(libKotlin.plugins.compose.multiplatform)
    // Compose Multiplatform 编译器插件
    alias(libKotlin.plugins.compose.compiler)
    // alias(privateLibJava.plugins.java.version)
    // alias(privateLibJava.plugins.publish.private)
}

tasks.withType<Test> {
    // 连接 Gradle 测试任务与 JUnit 工具
    useJUnitPlatform()
}

dependencies {
    api(project(":adb_cast_local"))
    api(privateLibKotlin.external.adb.core)

    api(libJava.slf4j.api)

    // Compose Desktop（包含 Skia、Foundation、Material 等基础组件）
    api(compose.desktop.currentOs)
    // Material3
    api(libKotlin.compose.material3)
    // 协程（Skia/Compose 异步绘制）
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.ktx.coroutines.swing)


    // JUnit5 BOM 版本配置文件
    testImplementation(platform(libJava.junit5.bom))
    // JUnit5 平台启动器
    testImplementation(libJava.junit5.launcher)
    // Jupiter（JUnit5 引擎的实现）
    testImplementation(libJava.junit5.jupiter)
}

// javaVersionConfig {
//     jdkVersion = JavaVersion.VERSION_1_8
// }

// privatePublishConfig {
//     groupID = mvnGroupID
//     artifactID = mvnArtifactID
//     version = mvnVersion
// }
