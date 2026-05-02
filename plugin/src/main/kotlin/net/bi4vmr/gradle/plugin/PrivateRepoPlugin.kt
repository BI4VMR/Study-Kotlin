package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.MavenRepos
import net.bi4vmr.gradle.entity.MavenRepo
import net.bi4vmr.gradle.util.LogUtil
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

        // 插件配置阶段为单线程执行，不必考虑全局变量的同步问题。
        addRepo(target.repositories, requireNotNull(netTestResult))
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
