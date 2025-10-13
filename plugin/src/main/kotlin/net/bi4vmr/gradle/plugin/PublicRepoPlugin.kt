package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.MavenRepos
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.repositories

/**
 * Maven公共仓库插件。
 *
 * 自动为子模块添加常用的公共仓库。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PublicRepoPlugin : Plugin<Project> {

    companion object {

        const val NAME: String = "net.bi4vmr.gradle.plugin.repo.public"

        // 预设仓库列表
        private val REPOS = listOf(
            MavenRepos.PUBLIC_TENCENT,
            MavenRepos.PUBLIC_ALIYUN,
            MavenRepos.GOOGLE_ALIYUN,
            MavenRepos.SPRING_ALIYUN,
            MavenRepos.JITPACK
        )
    }

    override fun apply(target: Project) {
        target.repositories {
            REPOS.forEach {
                maven {
                    setUrl(it.url)
                    if (it.url.startsWith("http://")) {
                        isAllowInsecureProtocol = true
                    }

                    if (it.username != null && it.password != null) {
                        credentials {
                            username = it.username
                            password = it.password
                        }
                    }
                }
            }

            mavenCentral()
            google()
        }
    }
}
