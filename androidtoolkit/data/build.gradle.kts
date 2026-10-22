plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    /* 私有组件 */
    api(project(":lib_external:adb_proxy_common"))
    // api(privateLibKotlin.external.adb.proxy.common)


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.datastore.preferences)
}
