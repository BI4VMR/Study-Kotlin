package net.bi4vmr.androidtoolkit.core

import kotlinx.coroutines.flow.StateFlow
import net.bi4vmr.androidtoolkit.core.common.ExecutorType

/**
 * ADB 执行器公共能力接口。
 *
 * 本地与远程执行器通过该接口向上层提供统一的设备操作能力。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface ADBExecutor {

    /**
     * 获取执行器唯一标识。
     */
    fun getKey(): String

    fun getType(): ExecutorType

    fun getDisplayName(): String

    /**
     * Flow : 执行器是否可用（本地服务已启动或远程连接已建立）。
     */
    fun getAvailableFlow(): StateFlow<Boolean>

    /**
     * Flow : 设备列表。
     */
    fun getDevicesFlow(): StateFlow<List<IDevice>>

    /**
     * 尝试重新连接，本地执行器无需实现。
     */
    fun reconnect() {}

    /**
     * 释放资源。
     */
    fun release() {}
}
