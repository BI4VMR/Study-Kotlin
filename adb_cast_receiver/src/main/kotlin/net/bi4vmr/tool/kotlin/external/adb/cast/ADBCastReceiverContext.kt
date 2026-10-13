package net.bi4vmr.tool.kotlin.external.adb.cast

/**
 * 屏幕投射上下文（发送端）。
 *
 * 组织投屏过程中所使用的相关资源。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal data class ADBCastReceiverContext(

    /**
     * ADB 设备。
     */
    val deviceID: String,
)
