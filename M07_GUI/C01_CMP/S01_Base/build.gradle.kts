plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.compose.multiplatform)
    alias(libKotlin.plugins.compose.compiler)
}


dependencies {
    implementation(compose.desktop.currentOs)
}
