plugins {
    alias(libKotlin.plugins.core)
    // alias(libKotlin.plugins.compose)
}

// compose.desktop {
//     application {
//         mainClass = "net.bi4vmr.study.LogStatComposeKt"
//     }
// }

dependencies {
    implementation(privateLibJava.common.base)

    implementation(libKotlin.standardlib)
    implementation(libKotlin.ktx.coroutines.core)
    implementation(libJava.okhttps.core)
    implementation(libJava.gson)
    implementation(libJava.drewnoakes.metadataExtractor)
    implementation(libJava.apacheCommons.imaging)
    // Compose Desktop（包含当前操作系统所需的原生库）
    // implementation(compose.desktop.currentOs)
    // implementation("androidx.compose.ui:ui-desktop:1.7.0")
    implementation("com.google.android.tools:ddmlib:r13")

    implementation("org.bytedeco:javacv:1.5.10")
    implementation("org.bytedeco:ffmpeg-platform:6.1.1-1.5.10")
}
