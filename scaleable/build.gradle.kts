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
    application {
        mainClass = "net.bi4vmr.study.scaleable.MainKt"
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libKotlin.compose.material3)
}
