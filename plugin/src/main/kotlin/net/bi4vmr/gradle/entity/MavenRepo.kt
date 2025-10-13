package net.bi4vmr.gradle.entity

/**
 * Maven仓库。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class MavenRepo(
    val name: String,
    val url: String,
    val description: String? = null,
    val username: String? = null,
    val password: String? = null
)
