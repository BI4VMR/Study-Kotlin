@file:Suppress("UnstableApiUsage")

// 构建工具的依赖配置
pluginManagement {
    // 加载自定义插件
    includeBuild("plugin")

    // 声明 Gradle 插件仓库
    repositories {
        // 阿里云仓库镜像： Gradle 社区插件
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
        // 阿里云仓库镜像： Maven 中心仓库 + JCenter
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // 阿里云仓库镜像： Google 仓库
        maven {
            setUrl("https://maven.aliyun.com/reposiory/google/")
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        // 腾讯云仓库镜像： Maven 中心仓库 + Google + JCenter
        maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        gradlePluginPortal()
        mavenCentral()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
    }
}

// 所有模块的依赖配置
dependencyResolutionManagement {
    // 版本管理配置
    versionCatalogs {
        // 公共组件 (Java)
        create("libJava") {
            from(files("misc/version/dependency_public_java.toml"))
        }

        // 公共组件 (Kotlin)
        create("libKotlin") {
            from(files("misc/version/dependency_public_kotlin.toml"))
        }

        // 私有组件 (Java)
        create("privateLibJava") {
            from(files("misc/version/dependency_private_java.toml"))
        }

        // 私有组件 (Kotlin)
        create("privateLibKotlin") {
            from(files("misc/version/dependency_private_kotlin.toml"))
        }

        // Android 版本配置
        create("agp") {
            from(files("misc/version/agp.toml"))
        }
    }
}


/* ----- 项目结构声明 ----- */
// 主工程名称
rootProject.name = "Study-Kotlin"

// ----- 基础知识 -----
include("M01_Overview:C00_Temporary")
include("M01_Overview:C01_HelloWorld")

// ----- 基本语法 -----
include(":M02_Syntax:C01_Struct")
include(":M02_Syntax:C02_Operator")
include(":M02_Syntax:C03_Control")
include(":M02_Syntax:C04_Method")
include(":M02_Syntax:C05_Array")
include(":M02_Syntax:C06_Exception")
include(":M02_Syntax:C07_Terminal")

// ----- 高级特性 -----
include(":M03_Advance:C01_OOP:S01_Base")
include(":M03_Advance:C01_OOP:S02_Encapsulation")
include(":M03_Advance:C01_OOP:S03_Inherit")
include(":M03_Advance:C01_OOP:S04_Polymorphism")
include(":M03_Advance:C01_OOP:S05_Interface")
include(":M03_Advance:C01_OOP:S06_EnumClass")
include(":M03_Advance:C02_Features:S01_Generics")
include(":M03_Advance:C02_Features:S02_Annotation")
include(":M03_Advance:C02_Features:S03_Reflection")
include(":M03_Advance:C02_Features:S04_Functional")
include(":M03_Advance:C04_MultiTask:S01_Thread")
include(":M03_Advance:C04_MultiTask:S02_Coroutine")
include(":M03_Advance:C04_MultiTask:S03_Tool")

// ----- 实用工具 -----
include(":M04_Utils:C01_Test:S01_Base")
include(":M04_Utils:C01_Test:S02_Mockito")
include(":M04_Utils:C01_Test:S03_MockK")

// ----- 用户界面 -----
include(":M07_GUI:C01_CMP:S01_Desktop")


// TODO Test
include(":M01_Overview:C02_AdbMqtt:Protocol")
include(":M01_Overview:C02_AdbMqtt:Agent")
include(":M01_Overview:C02_AdbMqtt:Client")

include(":lib_external:adb_cast_common")
include(":lib_external:adb_cast_media")
include(":lib_external:adb_cast_local")
include(":lib_external:adb_cast_transfer")
include(":lib_external:adb_cast_receiver")
include(":lib_external:adb_cast_compose")
include(":lib_external:adb_proxy_api")
include(":lib_external:adb_proxy_client")
include(":lib_external:adb_proxy_server")

include(":demo_external:adb_cast_compose_local")
include(":demo_external:adb_cast_compose_remote")
include(":demo_external:adb_proxy_server")
include(":demo_external:adb_proxy_client")

include(":adb_main")
include(":adb_gui")
include(":adb_core")
