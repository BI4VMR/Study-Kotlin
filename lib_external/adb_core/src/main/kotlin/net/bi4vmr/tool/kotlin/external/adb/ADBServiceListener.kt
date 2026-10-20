package net.bi4vmr.tool.kotlin.external.adb

import net.bi4vmr.tool.kotlin.external.adb.common.ServiceState

/**
 * ADB 服务监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBServiceListener {

    /**
     * ADB服务就绪事件。
     *
     * 同步执行 [ADBController] 方法时不需要关心本回调；异步执行 [ADBController.init] 方法时可以通过本回调监听初始化状态。
     *
     * @param[state] 初始化状态，`true` 表示初始化成功，`false` 表示初始化失败。
     */
    fun onStateChange(state: ServiceState) {
        // 默认不作任何操作
    }
}
