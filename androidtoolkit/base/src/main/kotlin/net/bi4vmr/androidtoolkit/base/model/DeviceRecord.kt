package net.bi4vmr.androidtoolkit.base.model

import net.bi4vmr.tool.java.common.base.id.IDUtil
import java.time.ZonedDateTime

/**
 * 实体类：远程设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DeviceRecord(

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
     * 创建时间。
     */
    val createTime: ZonedDateTime = ZonedDateTime.now(),

    /**
     * 更新时间。
     */
    val updateTime: ZonedDateTime = createTime,

    /**
     * 外键：所属执行器。
     */
    val executorID: String
)
