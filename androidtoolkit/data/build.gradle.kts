plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.sqldelight)
}

dependencies {
    api(project(":androidtoolkit:base"))

    /* 私有组件 */
    api(project(":lib_external:adb_proxy_common"))
    // api(privateLibKotlin.external.adb.proxy.common)
    implementation(privateLibJava.common.base)
    implementation(privateLibKotlin.common.base)

    /* 外部组件 */
    implementation(libKotlin.ktx.coroutines.core)
    implementation(libKotlin.datastore.preferences)
    implementation(libKotlin.sqldelight.driver.sqlite)
    implementation(libKotlin.sqldelight.coroutines)
}

sqldelight {
    databases {
        create("MainDatabase") {
            packageName.set("net.bi4vmr.androidtoolkit.data.sql")
        }
    }
}
