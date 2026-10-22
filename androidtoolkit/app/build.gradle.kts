import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val version = "1.0.1"


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

        nativeDistributions {
            packageName = "androidtoolkit"
            packageVersion = version
            description = "Android 调试工具箱"
            vendor = "BI4VMR"

            modules("java.sql", "jdk.security.auth")

            // 指定程序资源目录
            appResourcesRootDir.set(rootProject.layout.projectDirectory.dir("androidtoolkit/resources"))

            // MacOS 的 DMG 包需要签名才能运行，因此暂不支持 DMG 包，仅保留可执行目录压缩包方式。
            targetFormats(TargetFormat.Exe, TargetFormat.Deb)
        }
    }
}

dependencies {
    /* 工程组件 */
    implementation(project(":androidtoolkit:ui"))


    /* 外部组件 */
    runtimeOnly(libJava.slf4j.simple)
    implementation(compose.desktop.currentOs)
}
