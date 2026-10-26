val depInTOML: MinimalExternalModuleDependency = privateLibKotlin.external.adb.proxy.server.get()
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

dependencies {
    /* 工程组件 */
    // 本地调试
    api(project(":lib_external:adb_proxy_common"))
    api(project(":lib_external:adb_core"))

    // 依赖传递
    // api(privateLibKotlin.external.adb.proxy.common)
    // api(privateLibKotlin.external.adb.core)


    /* 外部组件 */
    api(libJava.slf4j.api)
    api(libJava.pty4j)
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.ktor.server.core)
    api(libKotlin.ktor.server.cio)
    api(libKotlin.ktor.server.netty)
    api(libKotlin.ktor.server.websockets)
    api(libKotlin.ktor.network.tls.certificates)
    api(libKotlin.ktor.server.content.negotiation)
    api(libKotlin.ktor.serialization.kotlinx.json)


    /* 测试组件 */
    // JUnit5 BOM 版本配置文件
    testImplementation(platform(libJava.junit5.bom))
    // JUnit5 平台启动器
    testImplementation(libJava.junit5.launcher)
    // Jupiter（JUnit5 引擎的实现）
    testImplementation(libJava.junit5.jupiter)
}

javaVersionConfig {
    // PTY4J 要求 JDK 版本至少为 11 。
    jdkVersion = JavaVersion.VERSION_11
}

privatePublishConfig {
    groupID = mvnGroupID
    artifactID = mvnArtifactID
    version = mvnVersion
}
