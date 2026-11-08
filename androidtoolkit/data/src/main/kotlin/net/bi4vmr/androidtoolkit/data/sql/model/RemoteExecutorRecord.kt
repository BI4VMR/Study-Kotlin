package net.bi4vmr.androidtoolkit.data.sql.model

import net.bi4vmr.tool.java.common.base.id.IDUtil

/**
 * 实体类：远程执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class RemoteExecutorRecord(

    /**
     * ID.
     */
    val id: String = IDUtil.GUIDv7(),

    /**
     * 目标地址。
     */
    val host: String,

    /**
     * 目标端口。
     */
    val port: Int,

    /**
     * 是否启用 TLS 。
     */
    val enableTLS: Boolean,

    /**
     * 创建时间 。
     */
    val createTime: Long = System.currentTimeMillis()
)
