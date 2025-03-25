plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)

    testImplementation(libJava.junit4)
    testImplementation(libKotlin.mockk)
}
