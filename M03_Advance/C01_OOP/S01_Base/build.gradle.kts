plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.stdlib)
    implementation(libKotlin.ktx.coroutines)
}
