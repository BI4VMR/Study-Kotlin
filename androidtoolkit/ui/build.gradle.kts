plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.compose.multiplatform)
    alias(libKotlin.plugins.compose.compiler)
}

compose.resources {
    // 资源类默认是 `internal` 修饰符，若要让调用者也可以引用，需要设置为公开的。
    publicResClass = true
    // 默认资源类名为 `<工程名称><包名>generated.resources.Res`，可以修改为符合习惯的包名。
    packageOfResClass = "net.bi4vmr.androidtoolkit.ui"
}

dependencies {
    /* 工程组件 */
    api(project(":androidtoolkit:core"))


    /* 私有组件 */
    api(project(":lib_external:adb_core"))
    api(project(":lib_external:adb_ktx"))
    // api(privateLibKotlin.external.adb.core)
    // api(privateLibKotlin.external.adb.ktx)
    api(project(":lib_external:adb_cast_compose_local"))
    api(project(":lib_external:adb_cast_compose_remote"))


    /* 外部组件 */
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.ktx.coroutines.swing)
    api(compose.desktop.currentOs)
    api(libKotlin.compose.material3)
    api(libKotlin.compose.material.icons)
    api(libKotlin.compose.viewmodel)
    api(libKotlin.compose.resources)
}
