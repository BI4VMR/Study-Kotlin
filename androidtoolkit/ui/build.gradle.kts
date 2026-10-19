plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.compose.multiplatform)
    alias(libKotlin.plugins.compose.compiler)
}

dependencies {
    api(project(":androidtoolkit:core"))


    /* 私有组件 */
    api(privateLibKotlin.external.adb.core)
    api(privateLibKotlin.external.adb.ktx)


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.ktx.coroutines.swing)
    api(compose.desktop.currentOs)
    api(libKotlin.compose.material3)
    api(libKotlin.compose.material.icons)
}
