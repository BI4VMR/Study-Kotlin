package net.bi4vmr.gradle.plugin

/**
 * 私有Maven发布插件配置项。
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
}
