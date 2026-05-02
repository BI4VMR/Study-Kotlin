package net.bi4vmr.gradle.plugin

import net.bi4vmr.gradle.data.Plugins
import net.bi4vmr.gradle.util.LogUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/**
 * Java版本插件。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class JavaVersionPlugin : Plugin<Project> {

    companion object {

        /**
         * 插件ID
         */
        const val NAME: String = "net.bi4vmr.gradle.plugin.java.version"
    }

    override fun apply(target: Project) {
        // 注册扩展
        target.extensions.create(JavaVersionConfig.NAME, JavaVersionConfig::class.java)

        target.plugins.withId(Plugins.JAVA_LIBRARY) {
            setVersion(target)
        }
        target.plugins.withId(Plugins.JAVA_APPLICATION) {
            setVersion(target)
        }
    }

    private fun setVersion(project: Project) {
        project.afterEvaluate {
            val ext = project.extensions.findByType(JavaVersionConfig::class.java)
                ?: throw IllegalArgumentException("Please use `javaVersionConfig {}` to register maven group and name info!")

            val version = ext.jdkVersion
            LogUtil.info("Use $version as target Java version.")

            project.extensions.configure<JavaPluginExtension> {
                sourceCompatibility = version
                targetCompatibility = version
            }
        }
    }
}
