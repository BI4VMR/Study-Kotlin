plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.stdlib)
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:logging-interceptor")
}
