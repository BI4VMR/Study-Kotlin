plugins {
    alias(libKotlin.plugins.core)
}

tasks.withType<Test> {
    // 连接 Gradle 测试任务与 JUnit 工具
    useJUnitPlatform()
}

dependencies {
    /* 工程组件 */
    api(project(":androidtoolkit:base"))
    implementation(project(":androidtoolkit:data"))


    /* 私有组件 */
    api(privateLibJava.common.base)
    // api(privateLibKotlin.external.adb.core)
    // api(privateLibKotlin.external.adb.ktx)
    api(project(":lib_external:adb_core"))
    api(project(":lib_external:adb_ktx"))
    api(project(":lib_external:adb_proxy_server"))
    api(project(":lib_external:adb_proxy_server_cast"))
    api(project(":lib_external:adb_proxy_server_pair"))
    api(project(":lib_external:adb_proxy_client"))
    api(project(":lib_external:adb_cast_local"))
    api(project(":lib_external:adb_cast_transfer"))
    api(project(":lib_external:adb_cast_receiver"))


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
    implementation(libKotlin.mcp.server)
    implementation(libKotlin.ktor.server.cio)


    /* 测试组件 */
    // JUnit5 BOM 版本配置文件
    testImplementation(platform(libJava.junit5.bom))
    // JUnit5 平台启动器
    testImplementation(libJava.junit5.launcher)
    // Jupiter（JUnit5 引擎的实现）
    testImplementation(libJava.junit5.jupiter)
}
