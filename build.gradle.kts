// Gradle 插件声明
plugins {
    alias(libKotlin.plugins.core).apply(false)
    alias(libKotlin.plugins.kotlin.multiplatform).apply(false)
    alias(libKotlin.plugins.kotlin.multiplatform.android).apply(false)
    alias(libKotlin.plugins.compose.multiplatform).apply(false)
    alias(libKotlin.plugins.compose.compiler).apply(false)
    alias(libKotlin.plugins.dokka).apply(false)

    alias(privateLibJava.plugins.repo.private).apply(false)
    alias(privateLibJava.plugins.repo.public).apply(false)
    alias(privateLibJava.plugins.publish.private).apply(false)
    alias(privateLibJava.plugins.publish.private.kmp).apply(false)
}


val pluginMavenRepoPrivate: String = privateLibJava.plugins.repo.private.get().pluginId
val pluginMavenRepoPublic: String = privateLibJava.plugins.repo.public.get().pluginId

// 为子工程应用自定义插件
allprojects {
    project.pluginManager.apply(pluginMavenRepoPrivate)
    project.pluginManager.apply(pluginMavenRepoPublic)
}
