plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.dokka)
}

dependencies {
    implementation(libKotlin.standardlib)
}
