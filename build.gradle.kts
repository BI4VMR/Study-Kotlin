// Gradle插件声明
plugins {
    alias(libKotlin.plugins.core).apply(false)
    alias(libKotlin.plugins.dokka).apply(false)

    alias(privateLibJava.plugins.repo.private).apply(false)
    alias(privateLibJava.plugins.repo.public).apply(false)
}

val pluginMavenRepoPrivate: String = privateLibJava.plugins.repo.private.get().pluginId
val pluginMavenRepoPublic: String = privateLibJava.plugins.repo.public.get().pluginId

// 为子工程应用自定义插件
allprojects {
    project.apply(plugin = pluginMavenRepoPrivate)
    project.apply(plugin = pluginMavenRepoPublic)
}
