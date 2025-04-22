package net.bi4vmr.study

import com.pateo.plugin.base.BasePlugin
import com.pateo.plugin.findFileFromScript
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler

class MavenRepoPlugin : BasePlugin<Project>() {

    companion object {
        private val repo: MutableList<MavenRepo> = mutableListOf()

        /**
         * 预置公共仓库，私有仓库请在```maven_config.json```中添加
         */
        val PRE_REPO = mutableListOf(
            MavenRepo(
                alias = "Local-Tencent",
                repoUrl = "http://127.0.0.1:8081/repository/maven-mirror-tencent/"
            ),
            MavenRepo(
                alias = "Local-Jitpack",
                repoUrl = "http://127.0.0.1:8081/repository/maven-mirror-jitpack/"
            ),
            MavenRepo(
                alias = "JITPACK",
                repoUrl = "https://jitpack.io"
            ),
            MavenRepo(
                alias = "ALIYUN_GOOGLE",
                repoUrl = "https://maven.aliyun.com/repository/google"
            ),
            MavenRepo(
                alias = "ALIYUN_CENTRAL",
                repoUrl = "https://maven.aliyun.com/repository/central"
            ),
            MavenRepo(
                alias = "ALIYUN_PUBLIC",
                repoUrl = "https://maven.aliyun.com/repository/public"
            ),
            MavenRepo(
                alias = "PATEO_PUBLIC",
                repoUrl = "http://10.10.96.219:8081/repository/maven-public"
            ),
            MavenRepo(
                alias = "PATEO_GOOGLE",
                repoUrl = "http://10.10.96.219:8081/repository/google"
            ),
            MavenRepo(
                alias = "PATEO_MAVEN",
                repoUrl = "http://10.10.96.219:8081/repository/pateo"
            ),
            MavenRepo(
                alias = "PATEO_NJ",
                repoUrl = "http://10.10.96.219:8081/repository/pateo-nj"
            ),
            MavenRepo(
                alias = "NJ_PATEO",
                repoUrl = "http://10.10.96.219:8081/repository/nj-pateo"
            )
        )

        /**
         * 获取仓库信息
         */
        fun get(alias: String): MavenRepo {
            return repo.find { it.alias == alias } ?: throw Exception("Not found repo of [$alias]")
        }

        /**
         * 获取所有仓库信息
         */
        fun get(): List<MavenRepo> = repo
    }

    data class MavenRepo(
        // 别名 short name
        val alias: String,
        // url
        val repoUrl: String,
        // 用户名
        val repoUserName: String = "",
        // 密码
        val repoPassword: String = ""
    ) {
        override fun toString(): String {
            val maskedPassword = if (repoPassword.isNotEmpty()) "*".repeat(8) else ""
            return "MavenRepo(alias='$alias', repoUrl='$repoUrl', repoUserName='$repoUserName', repoPassword='$maskedPassword')"
        }
    }

    override fun loadProject() {
        if (repo.isNotEmpty()) {
            addRepos(project.repositories, false)
            return
        }
        repo.addAll(PRE_REPO)
        var mavenFile = DepConfig.get(project.name).mavenFile
        logProxy("readMavenConfig mavenFile[$mavenFile].")
        if (mavenFile == null) {
            logProxy("The Maven file does not exist. Try to find it by oneself.")
            runCatching {
                mavenFile = project.findFileFromScript("maven_config.json")
            }.onFailure {
                logError("maven_config.json not exists.")
                return
            }
        }

        mavenFile?.run {
            if (!this.exists()) {
                logError("${this.absolutePath} not exists.")
                return
            }

            logProxy("MavenFile Path[${this.absolutePath}]")

            val json = JsonSlurper().parseText(this.readText())
            val repoList = json as? List<Map<String, String>>
            repoList?.forEach {
                repo.add(
                    MavenRepo(
                        it["alias"]!!,
                        it["repoUrl"]!!,
                        it["repoUserName"] ?: "",
                        it["repoPassword"] ?: ""
                    )
                )
            }

            addRepos(project.repositories, true)
        }
    }

    private fun addRepos(handler: RepositoryHandler, showLog: Boolean) {
        with(handler) {
            repo.filter {
                it.repoUrl.isNotEmpty()
            }.forEach {
                maven {
                    isAllowInsecureProtocol = true
                    if (it.repoPassword.isNotEmpty() && it.repoUserName.isNotEmpty()) {
                        credentials {
                            username = it.repoUserName
                            password = it.repoPassword
                        }
                    }
                    if (showLog) {
                        logProxy("addRepos[$it]")
                    }
                    setUrl(it.repoUrl)
                }
            }
            google()
            mavenCentral()
        }
    }
}