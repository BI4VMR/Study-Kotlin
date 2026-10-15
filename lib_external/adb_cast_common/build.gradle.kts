val depInTOML: MinimalExternalModuleDependency = privateLibKotlin.external.adb.cast.common.get()
val mvnGroupID: String = requireNotNull(depInTOML.group)
val mvnArtifactID: String = depInTOML.name
val mvnVersion: String = requireNotNull(depInTOML.version)


plugins {
    alias(libKotlin.plugins.kotlin.multiplatform)
    alias(libKotlin.plugins.dokka)

    alias(privateLibJava.plugins.java.version)
    alias(privateLibJava.plugins.publish.private.kmp)
}

tasks.withType<Test> {
    // 连接 Gradle 测试任务与 JUnit 工具
    useJUnitPlatform()
}

kotlin {
    /* 桌面平台配置 */
    jvm()

    /* 各平台依赖配置 */
    sourceSets {
        commonMain.dependencies {
            api(libKotlin.standardlib)
        }

        jvmMain.dependencies {
            api(libJava.slf4j.api)
        }

        jvmTest.dependencies {
            // JUnit5 BOM 版本配置文件
            implementation(dependencies.platform(libJava.junit5.bom))
            // JUnit5 平台启动器
            implementation(libJava.junit5.launcher)
            // Jupiter（JUnit5 引擎的实现）
            implementation(libJava.junit5.jupiter)
        }
    }
}

javaVersionConfig {
    jdkVersion = JavaVersion.VERSION_1_8
}

privatePublishConfig {
    groupID = mvnGroupID
    artifactID = mvnArtifactID
    version = mvnVersion
}
