plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.sqldelight)
}

dependencies {
    /* 私有组件 */
    implementation(privateLibJava.common.base)
    implementation(privateLibKotlin.common.base)
}
