plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    /* 私有组件 */
    api(privateLibJava.common.base)
    api(privateLibKotlin.external.adb.core)
    api(privateLibKotlin.external.adb.ktx)
    api(project(":lib_external:adb_proxy_server"))
    api(project(":lib_external:adb_proxy_client"))


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
}
