package net.bi4vmr.androidtoolkit.base.model

import net.bi4vmr.tool.java.common.base.id.IDUtil
import java.time.ZonedDateTime

/**
 * 实体类：远程执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class ExecutorRecord(

    /**
     * ID.
     */
    val id: String = IDUtil.GUIDv7(),

    /**
     * 显示名称。
     */
    val name: String,

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
     * 创建时间。
     */
    val createTime: ZonedDateTime = ZonedDateTime.now(),

    /**
     * 修改时间。
     */
    val updateTime: ZonedDateTime = createTime
)
