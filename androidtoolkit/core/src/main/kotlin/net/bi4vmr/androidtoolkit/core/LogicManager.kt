package net.bi4vmr.androidtoolkit.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant

/**
 * 业务逻辑入口。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class LogicManager {

    /**
     * Flow : 执行器列表。
     */
    private val _executorsFlow: MutableStateFlow<List<ADBExecutor>> =
        // 本地执行器总是可用的，将其固定放置在列表首位。
        MutableStateFlow(listOf(LocalExecutor))

    /**
     * Flow : 执行器列表。
     */
    val executorsFlow: StateFlow<List<ADBExecutor>> = _executorsFlow

    init {
        ADBController.init()
    }

    /**
     * 添加远程执行器。
     *
     * 先通过 HTTP 请求验证服务端可达，成功后才加入执行器列表并建立事件连接。
     *
     * @return `true` 表示添加成功， `false` 表示连接失败或已存在。
     */
    suspend fun addExecutor(host: String, port: Int = Constant.PORT_SERVER_DEFAULT): Boolean {
        if (_executorsFlow.value.any { it.getKey() == "$host:$port" }) {
            return false
        }

        val client = ADBProxyClient(host, port)
        val reachable = try {
            withTimeout(5_000L) { client.getServerVersion() }
            true
        } catch (e: CancellationException) {
            client.release()
            throw e
        } catch (e: Exception) {
            false
        }

        if (!reachable) {
            client.release()
            return false
        }

        client.init()
        _executorsFlow.update { it + RemoteExecutor(host, port, client) }
        return true
    }

    /**
     * 释放所有远程执行器。
     */
    fun release() {
        _executorsFlow.value.forEach { it.release() }
    }
}
