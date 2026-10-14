val depInTOML: MinimalExternalModuleDependency = privateLibKotlin.external.adb.core.get()
val mvnGroupID: String = requireNotNull(depInTOML.group)
val mvnArtifactID: String = depInTOML.name
val mvnVersion: String = requireNotNull(depInTOML.version)


plugins {
    alias(libKotlin.plugins.core)
    application
    // alias(privateLibJava.plugins.java.version)
    // alias(privateLibJava.plugins.publish.private)
}

application {
    mainClass = "net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransferMainKt"
}

tasks.named<JavaExec>("run") {
    // 转发控制台输入，使 readln 可以阻塞等待
    standardInput = System.`in`
}

tasks.withType<Test> {
    // 连接 Gradle 测试任务与 JUnit 工具
    useJUnitPlatform()
}

dependencies {
    api(project(":lib_external:adb_cast_common"))

    api(privateLibKotlin.common.base)
    api(privateLibKotlin.external.adb.core)

    api(libJava.slf4j.api)
    runtimeOnly(libJava.slf4j.simple)


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
