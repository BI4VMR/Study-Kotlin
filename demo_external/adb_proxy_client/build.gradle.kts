plugins {
    // Kotlin JVM 平台插件
    alias(libKotlin.plugins.core)
    // Compose Multiplatform 插件
    alias(libKotlin.plugins.compose.multiplatform)
    // Compose Multiplatform 编译器插件
    alias(libKotlin.plugins.compose.compiler)
}

// Compose Desktop 配置项
compose.desktop {
    // 声明应用程序
    application {
        // 注册该应用程序的入口
        mainClass = "net.bi4vmr.demo.MainKt"
    }
}

dependencies {
    /* 工程组件 */
    // 本地调试
    implementation(project(":lib_external:adb_proxy_client"))

    // 打包调试
    // implementation(privateLibKotlin.external.adb.proxy.client)


    /* 外部组件 */
    runtimeOnly(libJava.slf4j.simple)
    implementation(compose.desktop.currentOs)
}
