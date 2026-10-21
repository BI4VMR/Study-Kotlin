val depInTOML: MinimalExternalModuleDependency = privateLibKotlin.external.adb.cast.proxy.server.get()
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
    implementation(project(":lib_external:adb_core"))
    implementation(project(":lib_external:adb_cast_transfer"))
    implementation(project(":lib_external:adb_proxy_server"))


    /* 外部组件 */
    implementation(libJava.slf4j.api)
    implementation(libKotlin.ktor.server.core)
    implementation(libKotlin.ktor.server.websockets)


    /* 测试组件 */
    testImplementation(platform(libJava.junit5.bom))
    testImplementation(libJava.junit5.launcher)
    testImplementation(libJava.junit5.jupiter)
}

javaVersionConfig {
    jdkVersion = JavaVersion.VERSION_11
}

privatePublishConfig {
    groupID = mvnGroupID
    artifactID = mvnArtifactID
    version = mvnVersion
}
