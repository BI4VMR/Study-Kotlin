// Gradle 插件声明
plugins {
    `kotlin-dsl`
}

repositories {
    // 腾讯云仓库镜像： Maven 中心仓库 + Spring + Google + JCenter
    maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    // 阿里云仓库镜像： Gradle 社区插件
    maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin/") }
    // 阿里云仓库镜像： Maven 中心仓库 + JCenter
    maven { setUrl("https://maven.aliyun.com/repository/public/") }
    // 阿里云仓库镜像： Google 仓库
    maven { setUrl("https://maven.aliyun.com/repository/google/") }

    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    compileOnly("com.android.tools.build:gradle-api:8.0.0")
}

gradlePlugin {
    plugins {
        /* Java 编译版本控制 */
        create("JavaVersion") {
            // 声明插件 ID
            id = "net.bi4vmr.gradle.plugin.java.version"
            // 声明入口类
            implementationClass = "net.bi4vmr.gradle.plugin.JavaVersionPlugin"
        }

        /* 公共 Maven 仓库 */
        create("PublicRepo") {
            id = "net.bi4vmr.gradle.plugin.repo.public"
            implementationClass = "net.bi4vmr.gradle.plugin.PublicRepoPlugin"
        }

        /* 私有 Maven 仓库 */
        create("PrivateRepo") {
            id = "net.bi4vmr.gradle.plugin.repo.private"
            implementationClass = "net.bi4vmr.gradle.plugin.PrivateRepoPlugin"
        }

        /* 私有 Maven Publish 配置 */
        create("PrivatePublish") {
            id = "net.bi4vmr.gradle.plugin.maven.publish"
            implementationClass = "net.bi4vmr.gradle.plugin.PrivatePublishPlugin"
        }

        /* 私有 Maven Publish 配置 (KMP) */
        create("PrivatePublishKMP") {
            id = "net.bi4vmr.gradle.plugin.maven.publish.kmp"
            implementationClass = "net.bi4vmr.gradle.plugin.PrivatePublishKMPPlugin"
        }
    }
}
