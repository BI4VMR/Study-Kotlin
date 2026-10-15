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
    implementation(project(":adb_gui"))

    implementation(compose.desktop.currentOs)
}
