plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)
    // 协程核心
    implementation(libKotlin.ktx.coroutines.core)

    testImplementation(libJava.junit4)
    // 协程测试工具
    testImplementation(libKotlin.ktx.coroutines.test)
}
