/* Gradle插件配置 */
pluginManagement {
    // 插件仓库
    repositories {
        // 本地私有仓库与代理镜像，无法直连时应当禁用该配置。
        maven {
            isAllowInsecureProtocol = true
            setUrl("http://172.18.5.1:8081/repository/maven-union/")
        }

        mavenCentral()
        gradlePluginPortal()
    }
}

/* Maven依赖配置 */
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // Maven仓库
    repositories {
        // 本地私有仓库与代理镜像，无法直连时应当禁用该配置。
        maven {
            isAllowInsecureProtocol = true
            setUrl("http://172.18.5.1:8081/repository/maven-union/")
        }

        mavenCentral()
    }
}

/* ----- 项目结构声明 ----- */
// 主工程名称
rootProject.name = "Study-Kotlin"

include("M01-Overview")
