plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)
    implementation(libKotlin.ktx.coroutines.core)
    implementation(libJava.okhttps.core)
    implementation(libJava.gson)
}
