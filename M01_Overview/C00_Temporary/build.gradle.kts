plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)
    // implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    // implementation("com.squareup.okhttp3:logging-interceptor")
    implementation(libJava.okhttps.core)
    implementation(libJava.gson)
    implementation(libKotlin.ktx.coroutines)
}
