plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    /* 工程组件 */
    api(project(":androidtoolkit:data"))


    /* 私有组件 */
    api(privateLibJava.common.base)
    // api(privateLibKotlin.external.adb.core)
    // api(privateLibKotlin.external.adb.ktx)
    api(project(":lib_external:adb_core"))
    api(project(":lib_external:adb_ktx"))
    api(project(":lib_external:adb_proxy_server"))
    api(project(":lib_external:adb_proxy_server_cast"))
    api(project(":lib_external:adb_proxy_server_pair"))
    api(project(":lib_external:adb_proxy_client"))
    api(project(":lib_external:adb_cast_local"))
    api(project(":lib_external:adb_cast_transfer"))
    api(project(":lib_external:adb_cast_receiver"))


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
    implementation(libKotlin.mcp.server)
    implementation(libKotlin.ktor.server.cio)
}
