plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    /* 私有组件 */
    implementation(privateLibJava.common.base)
    implementation(privateLibKotlin.common.base)
}
