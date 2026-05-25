plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.compose)
}

compose.desktop {
    application {
        mainClass = "net.bi4vmr.study.LogStatComposeKt"
    }
}

// 强制使用 JetBrains Compose 的 ui-desktop（兼容 skiko 0.7.85.4 新 API），
// 排除 androidx.compose.ui:ui-desktop（旧 skiko API，与运行时不兼容）。
configurations.all {
    exclude(group = "androidx.compose.ui", module = "ui-desktop")
}

dependencies {
    implementation(libKotlin.standardlib)
    implementation(libKotlin.ktx.coroutines.core)
    implementation(libJava.okhttps.core)
    implementation(libJava.gson)
    implementation(privateLibJava.io.base)
    implementation(libJava.drewnoakes.metadataExtractor)
    implementation(libJava.apacheCommons.imaging)
    // Compose Desktop（包含当前操作系统所需的原生库）
    implementation(compose.desktop.currentOs)
    implementation("androidx.compose.ui:ui-desktop:1.7.0")
    implementation("com.google.android.tools:ddmlib:r13")
}
