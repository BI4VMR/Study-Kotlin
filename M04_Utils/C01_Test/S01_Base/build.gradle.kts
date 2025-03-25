plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)

    testImplementation(libKotlin.mockk)
    testImplementation(libJava.junit4)
}
