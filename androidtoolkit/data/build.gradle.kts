plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.datastore.preferences)
}
