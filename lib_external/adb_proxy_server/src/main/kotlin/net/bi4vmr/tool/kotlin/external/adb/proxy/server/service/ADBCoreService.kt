package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.ServerInfoResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import org.slf4j.LoggerFactory

/**
 * ADB 核心服务。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBCoreService(
    private val server: ADBProxyServer
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBCoreService::class.java)


    fun onGetServerInfo(): ServerInfoResponse {
        val conf = server.getRunningConfig()
        requireNotNull(conf) { "Running config not found! have you call `start()` at first?" }
        return ServerInfoResponse(conf.id, conf.name, Constants.PROTOCOL_VERSION)
    }
}
