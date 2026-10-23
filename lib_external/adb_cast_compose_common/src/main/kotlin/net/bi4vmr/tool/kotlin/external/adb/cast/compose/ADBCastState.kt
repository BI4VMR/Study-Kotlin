package net.bi4vmr.tool.kotlin.external.adb.cast.compose

/**
 * 投屏状态。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class ADBCastState {

    /**
     * 初始化。
     */
    IDLE,

    /**
     * 正在准备。
     */
    LOADING,

    /**
     * 正在投屏。
     */
    CASTING,

    /**
     * 出错终止。
     */
    ERROR;
}
