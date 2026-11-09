package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse
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


    fun getServerInfo(): ServerInfoResponse {
        val conf = server.getRunningConfig()
        requireNotNull(conf) { "Running config not found! have you call `start()` at first?" }
        return ServerInfoResponse(conf.name, Constants.PROTOCOL_VERSION)
    }

    fun getDeviceName(sn: String): BaseResponse<String> {
        return try {
            val name = requireDevice(sn).getName()
            BaseResponse(data = name)
        } catch (e: Exception) {
            BaseResponse(code = BaseResponse.CODE_FAILURE, message = e.message ?: "")
        }
    }

    fun getDeviceType(sn: String): BaseResponse<String> {
        return try {
            val state = requireDevice(sn).getState()
            BaseResponse(data = state.name)
        } catch (e: Exception) {
            BaseResponse(code = BaseResponse.CODE_FAILURE, message = e.message ?: "")
        }
    }

    private fun requireDevice(sn: String): LocalDevice {
        return ADBController.getDevice(sn) ?: throw IllegalStateException("Device not found for sn: $sn")
    }
}
