package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.MavenRepos
import net.bi4vmr.gradle.data.Plugins
import net.bi4vmr.gradle.entity.MavenRepo
import net.bi4vmr.gradle.util.LogUtil
import net.bi4vmr.gradle.util.NetUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * 私有 Maven 仓库发布插件 (Kotlin Multiplatform) 。
 *
 * 在 KMP 模块中， Publication 将会自动创建，此处仅对它们进行配置，不再创建新的 Publication 。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PrivatePublishKMPPlugin : Plugin<Project> {

    companion object {

        const val NAME: String = "net.bi4vmr.gradle.plugin.maven.publish.kmp"

        // KMP 根 Publication 名称。
        private const val PUBLICATION_ROOT: String = "kotlinMultiplatform"

        // Dokka HTML 文档发布任务名称。
        private const val DOKKA_HTML_PUBLICATION_TASK: String = "dokkaGeneratePublicationHtml"

        // 全局保存首次网络测试结果，避免每个子模块应用本插件都测试网络导致速度缓慢。
        private var netTestResult: MavenRepo? = null
    }

    override fun apply(target: Project) {
        // 检查仓库是否可用
        if (netTestResult == null) {
            if (NetUtil.scanByTCP(MavenRepos.PRIVATE_LAN.host, MavenRepos.PRIVATE_LAN.port)) {
                LogUtil.info("Use LAN address to connect private repositories.")
                netTestResult = MavenRepos.PRIVATE_LAN
            } else if (NetUtil.scanByTCP(MavenRepos.PRIVATE_HOSTNAME.host, MavenRepos.PRIVATE_HOSTNAME.port)) {
                LogUtil.info("Use Hostname to connect private repositories.")
                netTestResult = MavenRepos.PRIVATE_HOSTNAME
            } else if (NetUtil.scanByTCP(MavenRepos.PRIVATE_DYNV6.host, MavenRepos.PRIVATE_DYNV6.port)) {
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

        // 应用 Maven Publish 插件
        target.pluginManager.apply(Plugins.MAVEN_PUBLISH)

        // 注册扩展
        target.extensions.create(PrivatePublishConfig.NAME, PrivatePublishConfig::class.java)

        target.plugins.withId(Plugins.MAVEN_PUBLISH) {
            target.afterEvaluate {
                if (!target.plugins.hasPlugin(Plugins.KOTLIN_MULTIPLATFORM)) {
                    throw IllegalArgumentException("This plugin needs Kotlin Multiplatform!")
                }

                val ext = target.extensions.findByType(PrivatePublishConfig::class.java)
                    ?: throw IllegalArgumentException("Please use `privatePublishConfig {}` to register maven group and name info!")

                // 检查是否设置了必填属性
                val configGroupID: String = ext.groupID
                    ?: throw IllegalArgumentException("Please set 'groupID' in `privatePublishConfig {}`!")
                val configArtifactID: String = ext.artifactID
                    ?: throw IllegalArgumentException("Please set 'artifactID' in `privatePublishConfig {}`!")
                val configVersion: String? = ext.version


                // KMP 生成的产物继承模块的 Group 和版本号属性，直接设置 `maven-publish` 的属性是无效的。
                target.group = configGroupID
                configVersion?.let { target.version = it }

                // KMP 默认会生成源码包，因此仅当需要关闭时才需要调用该方法。
                if (!ext.uploadSources) {
                    target.extensions.configure<KotlinMultiplatformExtension> {
                        withSourcesJar(false)
                    }
                }

                // 文档由 Dokka 生成，打包为 `javadoc.jar` 并附加至所有 Publication 。
                var docTask: TaskProvider<Jar>? = null
                if (ext.uploadJavadoc) {
                    if (target.plugins.hasPlugin(Plugins.DOKKA)) {
                        val dokkaHtml = target.tasks.named(DOKKA_HTML_PUBLICATION_TASK)
                        docTask = target.tasks.register("javadocJar", Jar::class.java) {
                            archiveClassifier.set("javadoc")
                            from(dokkaHtml)
                            dependsOn(dokkaHtml)
                        }
                    } else {
                        LogUtil.info("Dokka is not applied in [${target.path}], ignore javadoc upload!")
                    }
                }

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

                    publications.withType<MavenPublication>().configureEach {
                        artifactId = configArtifactID
                        // 根模块使用基础 ArtifactID ，平台模块追加平台名称（ `<模块名称>-<平台名称>` ）。
                        artifactId = if (name == PUBLICATION_ROOT) {
                            configArtifactID
                        } else {
                            "$configArtifactID-$name"
                        }

                        val projectName: String = target.rootProject.name

                        // POM 信息
                        pom {
                            name.set(ext.artifactID)
                            url.set("https://github.com/BI4VMR/$projectName")
                            developers {
                                developer {
                                    name.set("BI4VMR")
                                    email.set("bi4vmr@outlook.com")
                                }
                            }
                        }

                        docTask?.let { artifact(it) }
                    }
                }
            }
        }
    }
}
