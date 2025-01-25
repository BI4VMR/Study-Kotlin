@file:Suppress("UnstableApiUsage")

// 构建工具的依赖配置
pluginManagement {
    // 声明Gradle插件仓库
    repositories {
        // 添加本地私有仓库与代理镜像，无法直连时应当禁用该配置。
        val hostName: String = java.net.InetAddress.getLocalHost().hostName
        println("Current host name is [$hostName]")
        var isInPrivateLAN = false
        run {
            java.net.NetworkInterface.getNetworkInterfaces().toList().forEach {
                it.inetAddresses.toList().forEach { addr ->
                    if ((addr is java.net.Inet4Address) && (addr.hostAddress.startsWith("172.18."))) {
                        isInPrivateLAN = true
                        return@run
                    }
                }
            }
        }
        println("Current host in private LAN? [$isInPrivateLAN]")

        if (hostName.startsWith("BI4VMR") && isInPrivateLAN) {
            println("Current host is in private network, add LAN repositorys.")
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-mirror-tencent/")
            }
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-private/")
            }
        } else {
            if (java.net.InetAddress.getByName("192.168.128.1").isReachable(2000)) {
                println("Current host is not in private network, add VPN repositorys.")
                maven {
                    isAllowInsecureProtocol = true
                    setUrl("http://192.168.128.1:8081/repository/maven-mirror-tencent/")
                }
                maven {
                    isAllowInsecureProtocol = true
                    setUrl("http://192.168.128.1:8081/repository/maven-private/")
                }
            } else {
                println("Current host is not in private network, add LOCAL repositorys.")
                mavenLocal()
            }
        }

        // 腾讯云仓库镜像：Maven中心仓库+Google+JCenter
        maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 阿里云仓库镜像：Maven中心仓库+JCenter
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // 阿里云仓库镜像：Gradle社区插件
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }

        mavenCentral()
        gradlePluginPortal()
    }
}

// 所有模块的依赖配置
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // 声明Maven组件仓库
    repositories {
        // 添加本地私有仓库与代理镜像，无法直连时应当禁用该配置。
        val hostName: String = java.net.InetAddress.getLocalHost().hostName
        var isInPrivateLAN = false
        run {
            java.net.NetworkInterface.getNetworkInterfaces().toList().forEach {
                it.inetAddresses.toList().forEach { addr ->
                    if ((addr is java.net.Inet4Address) && (addr.hostAddress.startsWith("172.18."))) {
                        isInPrivateLAN = true
                        return@run
                    }
                }
            }
        }

        if (hostName.startsWith("BI4VMR") && isInPrivateLAN) {
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-mirror-tencent/")
            }
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-jitpack/")
            }
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-private/")
            }
        } else {
            if (java.net.InetAddress.getByName("192.168.128.1").isReachable(2000)) {
                maven {
                    isAllowInsecureProtocol = true
                    setUrl("http://192.168.128.1:8081/repository/maven-mirror-tencent/")
                }
                maven {
                    isAllowInsecureProtocol = true
                    setUrl("http://192.168.128.1:8081/repository/maven-jitpack/")
                }
                maven {
                    isAllowInsecureProtocol = true
                    setUrl("http://192.168.128.1:8081/repository/maven-private/")
                }
            } else {
                mavenLocal()
            }
        }

        // 腾讯云仓库镜像：Maven中心仓库+Google+JCenter
        maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 阿里云仓库镜像：Maven中心仓库+JCenter
        maven { setUrl("https://maven.aliyun.com/repository/public/") }
        // 阿里云仓库镜像：Google
        maven { setUrl("https://maven.aliyun.com/repository/google/") }

        mavenCentral()
        google()
        maven { setUrl("https://jitpack.io/") }
    }

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


// ----- 基础知识 -----
include("M01_Overview:C00_Temporary")
include("M01_Overview:C01_HelloWorld")

// ----- 基本语法 -----
include(":M02_Syntax:C01_Struct")

// ----- 高级特性 -----
include(":M03_Advance:C01_OOP:S01_Base")
include(":M03_Advance:C01_OOP:S06_EnumClass")
include(":M03_Advance:C03_Concurrent:S02_Coroutine")
include(":M03_Advance:C02_Features:S01_Generics")
include(":M03_Advance:C02_Features:S02_Annotation")
include(":M03_Advance:C02_Features:S03_Reflection")
