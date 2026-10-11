plugins {
    alias(libKotlin.plugins.core)
    // alias(libKotlin.plugins.compose)
}

// compose.desktop {
//     application {
//         mainClass = "net.bi4vmr.study.LogStatComposeKt"
//     }
// }

tasks.withType<Test> {
    // 连接Gradle测试任务与JUnit工具
    useJUnitPlatform()
}

dependencies {
    implementation(privateLibJava.common.base)
    implementation(privateLibKotlin.external.adb.core)
    implementation(privateLibKotlin.external.adb.ktx)

    implementation(libKotlin.ktx.coroutines.core)
    implementation(libKotlin.ktor.server.core)
    implementation(libKotlin.ktor.server.cio)
    implementation(libJava.okhttps.core)
    implementation(libJava.gson)
    implementation(libJava.drewnoakes.metadataExtractor)
    implementation(libJava.apacheCommons.imaging)
    implementation(libJava.slf4j.api)
    implementation(libJava.slf4j.simple)

    implementation("org.bytedeco:javacv:1.5.10")
    implementation("org.bytedeco:ffmpeg-platform:6.1.1-1.5.10")

    // JUnit5 BOM版本配置文件
    testImplementation(platform(libJava.junit5.bom))
    // JUnit5 平台启动器
    testImplementation(libJava.junit5.launcher)
    // Jupiter（JUnit5引擎的实现）
    testImplementation(libJava.junit5.jupiter)
}
