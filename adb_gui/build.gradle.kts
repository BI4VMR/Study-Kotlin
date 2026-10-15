plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.compose.multiplatform)
    alias(libKotlin.plugins.compose.compiler)
}

dependencies {
    api(compose.desktop.currentOs)
    api(libKotlin.compose.material3)
}
