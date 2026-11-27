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
        mainClass = "net.bi4vmr.study.MainKt"
    }
}

dependencies {
    // Compose Desktop 运行时
    implementation(compose.desktop.currentOs)
    // Material3 组件
    implementation(libKotlin.compose.material3)
    // Material3 自适应布局・核心（WindowSizeClass 等）
    implementation(libKotlin.compose.adaptive.core)
    // Material3 自适应布局・面板（ListDetailPaneScaffold 等）
    implementation(libKotlin.compose.adaptive.layout)
    // Material3 自适应布局・导航（ThreePaneScaffoldNavigator 等）
    implementation(libKotlin.compose.adaptive.navigation)
}
