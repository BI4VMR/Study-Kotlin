package net.bi4vmr.gradle.plugin

/**
 * 私有 Maven 仓库发布插件配置项。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
open class PrivatePublishConfig {

    companion object {
        const val NAME = "privatePublishConfig"
    }

    /**
     * Maven GroupID。
     *
     * 必填属性。
     */
    var groupID: String? = null

    /**
     * Maven ArtifactID。
     *
     * 必填属性。
     */
    var artifactID: String? = null

    /**
     * Maven 组件版本号。
     */
    var version: String? = null

    /**
     * 是否上传源码包。
     *
     * 默认上传。
     */
    var uploadSources: Boolean = true

    /**
     * 是否上传文档包。
     *
     * 默认上传。
     */
    var uploadJavadoc: Boolean = true

    /**
     * Android 模块是否发布全部变体。
     *
     * - `false` : 发布 Release 变体。
     * - `true` : 发布全部变体，默认情况包括 `debug` 和 `release` 。
     */
    var includeAllVariants: Boolean = false
}
