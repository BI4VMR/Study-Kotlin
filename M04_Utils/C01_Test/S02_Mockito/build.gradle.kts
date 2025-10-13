plugins {
    alias(libKotlin.plugins.core)
}

dependencies {
    implementation(libKotlin.standardlib)

    testImplementation(libJava.junit4)
    testImplementation(libJava.mockito.core)
    testImplementation(libKotlin.mockito.kotlin)
}
