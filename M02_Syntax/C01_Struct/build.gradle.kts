plugins {
    alias(libs.plugins.kotlin.core)
    id("org.jetbrains.dokka") version "1.9.20"
}

dependencies {
    implementation(libs.kotlin.stdlib)
}
