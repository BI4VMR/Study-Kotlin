plugins {
    alias(libKotlin.plugins.core)
    application
}

dependencies {
    implementation(libKotlin.standardlib)
    implementation(project(":M01_Overview:C02_AdbMqtt:Protocol"))
}

application {
    mainClass = "net.bi4vmr.study.adbmqtt.agent.AgentMainKt"
}
