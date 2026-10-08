plugins {
    alias(libKotlin.plugins.core)
    application
}

dependencies {
    implementation(libKotlin.standardlib)
    implementation(project(":M01_Overview:C02_AdbMqtt:Protocol"))
}

// 允许交互式输入命令
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

application {
    mainClass = "net.bi4vmr.study.adbmqtt.client.ClientMainKt"
}
