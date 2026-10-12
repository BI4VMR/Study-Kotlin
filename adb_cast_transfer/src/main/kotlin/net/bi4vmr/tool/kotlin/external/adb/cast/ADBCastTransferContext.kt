package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice

/**
 * 屏幕投射上下文（发送端）。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class ADBCastTransferContext(

    /**
     * ADB 设备。
     */
    val device: ADBDevice,
)
