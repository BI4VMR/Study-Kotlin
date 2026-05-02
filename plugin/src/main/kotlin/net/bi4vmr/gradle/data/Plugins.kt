package net.bi4vmr.gradle.data

import net.bi4vmr.gradle.plugin.JavaVersionPlugin
import net.bi4vmr.gradle.plugin.PrivatePublishPlugin
import net.bi4vmr.gradle.plugin.PrivateRepoPlugin
import net.bi4vmr.gradle.plugin.PublicRepoPlugin

/**
 * 插件名称列表。
 *
 * 调用者可以通过常量引用插件。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Plugins {

    const val JAVA_LIBRARY: String = "java-library"

    const val JAVA_APPLICATION: String = "java-application"

    const val MAVEN_PUBLISH: String = "maven-publish"

    const val JACOCO: String = "jacoco"

    const val ANDROID_LIBRARY = "com.android.library"

    const val ANDROID_APPLICATION = "com.android.application"

    const val ANDROID_KOTLIN = "org.jetbrains.kotlin.android"

    const val JAVA_VERSION: String = JavaVersionPlugin.NAME

    const val PUBLIC_REPO: String = PublicRepoPlugin.NAME

    const val PRIVATE_REPO: String = PrivateRepoPlugin.NAME

    const val PRIVATE_PUBLISH: String = PrivatePublishPlugin.NAME
}
