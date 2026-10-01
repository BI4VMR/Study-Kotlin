package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.Plugins
import net.bi4vmr.gradle.util.LogUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Java SDK 版本配置插件。
 *
 * 统一配置 Java / Kotlin 等编译环境。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class JavaVersionPlugin : Plugin<Project> {

    companion object {

        /**
         * 插件 ID 。
         */
        const val NAME: String = "net.bi4vmr.gradle.plugin.java.version"
    }

    override fun apply(target: Project) {
        // 注册扩展
        target.extensions.create(JavaVersionConfig.NAME, JavaVersionConfig::class.java)

        // Java 工程
        target.pluginManager.withPlugin(Plugins.JAVA) {
            setJavaVersion(target)
        }

        // Kotlin 工程（该插件已依赖 Java 插件）。
        target.pluginManager.withPlugin(Plugins.KOTLIN_JVM) {
            setKotlinVersion(target)
        }
    }

    private fun setJavaVersion(project: Project) {
        project.afterEvaluate {
            val version = getConfig(project).jdkVersion
            LogUtil.info("Use $version as Java target version.")

            project.extensions.configure<JavaPluginExtension> {
                sourceCompatibility = version
                targetCompatibility = version
            }
        }
    }

    private fun setKotlinVersion(project: Project) {
        project.afterEvaluate {
            val version = getConfig(project).jdkVersion
            LogUtil.info("Use $version as Kotlin target version.")
            val target = JvmTarget.fromTarget(version.toString())

            project.extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions {
                    jvmTarget.set(target)
                }
            }
        }
    }

    private fun getConfig(project: Project): JavaVersionConfig {
        return project.extensions.findByType(JavaVersionConfig::class.java)
            ?: throw GradleException("Please use `javaVersionConfig {}` to config target version!")
    }
}
