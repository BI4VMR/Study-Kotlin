plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    /* 私有组件 */
    api(privateLibKotlin.external.adb.core)
    api(privateLibKotlin.external.adb.ktx)


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
}
