plugins {
    alias(libJava.plugins.java.application)
    alias(libKotlin.plugins.core)
}

application {
    mainClass = "net.bi4vmr.demo.ServerKt"
}

dependencies {
    /* 工程组件 */
    // 本地调试
    implementation(project(":lib_external:adb_proxy_server"))
    implementation(project(":lib_external:adb_proxy_server_cast"))

    // 打包调试
    // implementation(privateLibKotlin.external.adb.proxy.server)
    // implementation(privateLibKotlin.external.adb.proxy.server.cast)


    /* 外部组件 */
    runtimeOnly(libJava.slf4j.simple)
}
