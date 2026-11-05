package net.bi4vmr.tool.kotlin.external.adb.common.callback

import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState

/**
 * ADB 服务监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBServiceListener {

    /**
     * 事件：ADB 服务状态变更。
     *
     * @param[state] 服务状态。
     */
    fun onStateChange(state: ServiceState) {
        // 默认不作任何操作
    }

    /**
     * 事件：服务初始化失败。
     *
     * @param[error] 异常信息。
     */
    fun onInitFail(error: Exception?) {
        // 默认不作任何操作
    }
}
