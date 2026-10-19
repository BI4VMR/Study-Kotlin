plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.compose.multiplatform)
    alias(libKotlin.plugins.compose.compiler)
}

// Compose Desktop 配置项
compose.desktop {
    // 声明应用程序
    application {
        // 注册该应用程序的入口
        mainClass = "net.bi4vmr.androidtoolkit.app.MainKt"
    }
}

dependencies {
    /* 工程组件 */
    implementation(project(":androidtoolkit:ui"))


    /* 外部组件 */
    runtimeOnly(libJava.slf4j.simple)
    implementation(compose.desktop.currentOs)
}
