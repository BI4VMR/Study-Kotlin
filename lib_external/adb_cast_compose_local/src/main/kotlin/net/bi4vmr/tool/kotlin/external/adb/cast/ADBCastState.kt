package net.bi4vmr.tool.kotlin.external.adb.cast

/**
 * 投屏状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal enum class ADBCastState {

    /**
     * 准备中。
     */
    Loading,

    /**
     * 投屏中。
     */
    Casting,

    /**
     * 出错终止。
     */
    Error;
}
