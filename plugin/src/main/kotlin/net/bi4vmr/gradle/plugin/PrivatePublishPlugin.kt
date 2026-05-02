package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.MavenRepos
import net.bi4vmr.gradle.data.Plugins
import net.bi4vmr.gradle.entity.MavenRepo
import net.bi4vmr.gradle.util.LogUtil
import net.bi4vmr.gradle.util.NetUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * 私有Maven发布插件。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PrivatePublishPlugin : Plugin<Project> {

    companion object {

        const val NAME: String = "net.bi4vmr.gradle.plugin.maven.publish"

        // 全局保存首次网络测试结果，避免每个子模块应用本插件都测试网络导致速度缓慢。
        private var netTestResult: MavenRepo? = null
    }

    override fun apply(target: Project) {
        // 检查仓库是否可用
        if (netTestResult == null) {
            if (NetUtil.scanByTCP(MavenRepos.PRIVATE_LAN.host, MavenRepos.PRIVATE_LAN.port)) {
                LogUtil.info("Use LAN address to connect private repositories.")
                netTestResult = MavenRepos.PRIVATE_LAN
            } else if (NetUtil.scanByTCP(MavenRepos.PRIVATE_DYNV6.host, MavenRepos.PRIVATE_DYNV6.port)) {
                LogUtil.info("Use Hostname to connect private repositories.")
                netTestResult = MavenRepos.PRIVATE_HOSTNAME
            } else if (NetUtil.scanByTCP(MavenRepos.PRIVATE_HOSTNAME.host, MavenRepos.PRIVATE_HOSTNAME.port)) {
                LogUtil.info("Use DynV6 domain to connect private repositories.")
                netTestResult = MavenRepos.PRIVATE_DYNV6
            } else if (NetUtil.scanByTCP(MavenRepos.PRIVATE_LOCAL.host, MavenRepos.PRIVATE_LOCAL.port)) {
                LogUtil.info("Private repositories are not reachable, use local repositories.")
                netTestResult = MavenRepos.PRIVATE_LOCAL
            } else {
                LogUtil.info("Both private and local repositories are not reachable, use Maven local repository.")
                netTestResult = MavenRepos.PRIVATE_MAVEN_LOCAL
            }
        }

        // 应用Maven Publish插件
        target.pluginManager.apply(Plugins.MAVEN_PUBLISH)

        // 注册扩展
        target.extensions.create(PrivatePublishConfig.NAME, PrivatePublishConfig::class.java)

        target.plugins.withId(Plugins.MAVEN_PUBLISH) {
            target.afterEvaluate {
                val ext = target.extensions.findByType(PrivatePublishConfig::class.java)
                    ?: throw IllegalArgumentException("Please use `privatePublishConfig {}` to register maven group and name info!")

                // 检查是否设置了必填属性
                ext.groupID
                    ?: throw IllegalArgumentException("Please set 'groupID' in `privatePublishConfig {}`!")
                ext.artifactID
                    ?: throw IllegalArgumentException("Please set 'artifactID' in `privatePublishConfig {}`!")

                target.extensions.configure<PublishingExtension> {
                    repositories {
                        val repoURL = requireNotNull(netTestResult).url
                            // 读取地址为私有仓库与镜像仓库的聚合地址，因此写入地址需要替换为指定的私有仓库。
                            .replace("maven-union", "maven-private")

                        maven {
                            name = "Private"
                            isAllowInsecureProtocol = true
                            setUrl(repoURL)
                            credentials {
                                username = "uploader"
                                password = "uploader"
                            }
                        }
                    }

                    publications {
                        // 创建名为"Maven"的发布配置
                        register<MavenPublication>("Maven") {
                            // 产物的基本信息
                            groupId = ext.groupID
                            artifactId = ext.artifactID
                            version = ext.version

                            // 发布程序包
                            if (target.isAndroidLib()) {
                                from(components.getByName("release"))
                            } else {
                                from(components.getByName("java"))
                            }

                            val projectName: String = target.rootProject.name

                            // POM信息
                            pom {
                                // 打包格式
                                packaging = if (target.isAndroidLib()) "aar" else "jar"
                                name.set(ext.artifactID)
                                url.set("https://github.com/BI4VMR/$projectName")
                                developers {
                                    developer {
                                        name.set("BI4VMR")
                                        email.set("bi4vmr@outlook.com")
                                    }
                                }
                            }
                        }
                    }
                }

                // 根据模块类型配置是否上传源码包和文档包
                if (target.isAndroidLib()) {
                    /*
                     * 自从Gradle 7.0开始，Android Library默认会发布源码，且无法在 `afterEvaluate {}` 阶段修改配置，因此无法
                     * 通过插件的Extensions修改此行为，目前需要用户在 `android {}` 块中手动进行配置。
                     */
                    if (!ext.uploadSources || !ext.uploadJavadoc) {
                        throw IllegalArgumentException("This version of Gradle will upload sources automatically, plugin can not interrupt this behavior, please use `publishing {}` in `android {}` to config manually!")
                    }
                } else {
                    target.extensions.configure<JavaPluginExtension> {
                        if (ext.uploadSources) {
                            withSourcesJar()
                        }
                        if (ext.uploadJavadoc) {
                            withJavadocJar()

                            // 指定JavaDoc编码，解决系统编码与文件不一致导致错误。
                            target.tasks.withType(Javadoc::class.java).configureEach {
                                options.encoding = "UTF-8"
                            }
                        }
                    }
                }
            }
        }
    }

    // 判断当前模块是否为 Android Library 模块
    private fun Project.isAndroidLib(): Boolean {
        return plugins.hasPlugin(Plugins.ANDROID_LIBRARY)
    }
}
