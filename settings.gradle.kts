@file:Suppress("UnstableApiUsage")

// 构建工具的依赖配置
pluginManagement {
    // 声明Gradle插件仓库
    repositories {
        // 阿里云仓库镜像：Gradle社区插件
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
        // 阿里云仓库镜像：Maven中心仓库+JCenter
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // 阿里云仓库镜像：Google仓库
        maven { setUrl("https://maven.aliyun.com/repository/google/") }
        // 腾讯云仓库镜像：Maven中心仓库+Google+JCenter
        maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

// 所有模块的依赖配置
dependencyResolutionManagement {
    // 版本管理配置
    versionCatalogs {
        // 公共组件(Java)
        create("libJava") {
            from(files("misc/version/dependency_public_java.toml"))
        }

        // 公共组件(Kotlin)
        create("libKotlin") {
            from(files("misc/version/dependency_public_kotlin.toml"))
        }

        // 私有组件(Java)
        create("privateLibJava") {
            from(files("misc/version/dependency_private_java.toml"))
        }
    }
}

/* ----- 项目结构声明 ----- */
// 主工程名称
rootProject.name = "Study-Kotlin"
// 加载自定义插件
includeBuild("plugin")

// ----- 基础知识 -----
include("M01_Overview:C00_Temporary")
include("M01_Overview:C01_HelloWorld")

// ----- 基本语法 -----
include(":M02_Syntax:C01_Struct")
include(":M02_Syntax:C06_Exception")

// ----- 高级特性 -----
include(":M03_Advance:C01_OOP:S01_Base")
include(":M03_Advance:C01_OOP:S06_EnumClass")
include(":M03_Advance:C04_MultiTask:S02_Coroutine")
include(":M03_Advance:C02_Features:S01_Generics")
include(":M03_Advance:C02_Features:S02_Annotation")
include(":M03_Advance:C02_Features:S03_Reflection")

// ----- 实用工具 -----
include(":M04_Utils:C01_Test:S01_Base")
include(":M04_Utils:C01_Test:S02_Mockito")
include(":M04_Utils:C01_Test:S03_MockK")
