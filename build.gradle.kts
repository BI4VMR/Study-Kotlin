import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Gradle插件声明
plugins {
    alias(libs.plugins.kotlin.core)
}

group = "net.bi4vmr.study"
version = "1.0.0"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
