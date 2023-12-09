import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// 插件版本声明
plugins {
    kotlin("jvm") version "1.8.21"
}

group = "net.bi4vmr.study"
version = "1.0.0"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
