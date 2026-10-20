package net.bi4vmr.androidtoolkit.core.model

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.tool.kotlin.external.adb.common.ServiceState

/**
 * ADB 执行器公共能力接口。
 *
 * 本地与远程执行器通过该接口向上层提供统一的设备操作能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBExecutor {

    fun getType(): ExecutorType

    fun init() {}

    /**
     * 释放资源。
     */
    fun release() {}

    fun getDisplayName(): String

    /**
     * Flow : 执行器状态。
     */
    fun getStateFlow(): StateFlow<ServiceState>

    /**
     * Flow : 设备列表。
     */
    fun getDevicesFlow(): StateFlow<List<Device>>
}
