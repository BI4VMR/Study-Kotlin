package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.MavenRepos
import net.bi4vmr.gradle.entity.MavenRepo
import net.bi4vmr.gradle.util.NetUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Maven私有仓库插件。
 *
 * 自动为子模块添加常用的私有仓库。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PrivateRepoPlugin : Plugin<Project> {

    companion object {

        const val NAME: String = "net.bi4vmr.gradle.plugin.repo.private"

        private var netTestResult: MavenRepo? = null
    }

    override fun apply(target: Project) {
        // 如果网络测试结果为空，则先进行测试；否则根据测试结果选择仓库。
        if (netTestResult == null) {
            if (NetUtil.scanByTCP("172.16.5.1", 8081)) {
                println("Current host is in private network, add LAN repositories.")
                netTestResult = MavenRepos.PRIVATE_LAN
            } else if (NetUtil.scanByTCP("127.0.0.1", 8081)) {
                println("Current host is not in private network, add LOCAL repositories.")
                netTestResult = MavenRepos.PRIVATE_LOCAL
            } else {
                println("Current host is not in private network, add MAVEN_LOCAL repository.")
                netTestResult = MavenRepos.PRIVATE_MAVEN_LOCAL
            }

            // 插件配置阶段为单线程执行，不必考虑同步问题。
            addRepo(target.repositories, requireNotNull(netTestResult))
        } else {
            addRepo(target.repositories, requireNotNull(netTestResult))
        }
    }

    private fun addRepo(handler: RepositoryHandler, repo: MavenRepo) {
        // 特殊处理Maven本地仓库。
        if (repo == MavenRepos.PRIVATE_MAVEN_LOCAL) {
            handler.mavenLocal()
            return
        }

        with(handler) {
            maven {
                setUrl(repo.url)
                isAllowInsecureProtocol = true
            }
        }
    }
}
