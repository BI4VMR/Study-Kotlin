plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)
    implementation(libKotlin.ktx.coroutines)
}
