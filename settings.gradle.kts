// 构建工具的依赖配置
pluginManagement {
    // 声明Gradle插件仓库
    repositories {
        // 添加本地私有仓库与代理镜像，无法直连时应当禁用该配置。
        val hostName: String = java.net.InetAddress.getLocalHost().hostName
        println("Current host info is [$hostName]")
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
            println("Current host is in private network, add private repositorys.")
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-union/")
            }
        } else {
            println("Current host not in private network.")
        }

        // 腾讯云仓库镜像：Maven中心仓库
        maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 阿里云仓库镜像：Gradle社区插件
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }

        mavenCentral()
        gradlePluginPortal()

        mavenLocal()
    }
}

// 所有模块的依赖配置
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // 声明Maven组件仓库
    repositories {
        // 添加本地私有仓库与代理镜像，无法直连时应当禁用该配置。
        val hostName: String = java.net.InetAddress.getLocalHost().hostName
        println("Current host info is [$hostName]")
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
            println("Current host is in private network, add private repositorys.")
            maven {
                isAllowInsecureProtocol = true
                setUrl("http://172.18.5.1:8081/repository/maven-union/")
            }
        } else {
            println("Current host not in private network.")
        }

        // 腾讯云仓库镜像：Maven中心仓库
        maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        mavenCentral()

        mavenLocal()
    }

    // 版本管理配置
    versionCatalogs {
        // 声明命名空间"libs"
        create("libs") {
            // 导入依赖版本配置文件
            from(files("script/version/dependency.toml"))
        }
    }
}

/* ----- 项目结构声明 ----- */
// 主工程名称
rootProject.name = "Study-Kotlin"

/* ----- 基础知识 ----- */
include("M01_Overview:C00_Temporary")
include("M01_Overview:C01_HelloWorld")

/* ----- 基本语法 ----- */
include(":M02_Syntax:C01_Struct")

/* ----- 高级特性 ----- */
include(":M03_Advance:C01_OOP")
include(":M03_Advance:C03_Concurrent:S02_Coroutine")
