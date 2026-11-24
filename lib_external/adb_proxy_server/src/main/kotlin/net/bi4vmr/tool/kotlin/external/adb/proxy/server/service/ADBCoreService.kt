package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.ServerInfo
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.BaseResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import org.slf4j.LoggerFactory
import java.io.IOException

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

    private val adbObserver: ADBObserver = ADBObserver()


    fun init() {
        adbObserver.init()
    }

    fun release() {
        adbObserver.terminate()
    }

    fun getServerInfo(): ServerInfo {
        val conf = server.getRunningConfig()
        requireNotNull(conf) { "Running config not found! have you call `start()` at first?" }
        return ServerInfo(conf.name, Constants.PROTOCOL_VERSION)
    }

    fun getDeviceList(): List<RemoteDevice> = ADBController.getDevices()
        .map { device ->
            RemoteDevice(
                device.getSN(),
                device.getState()
            )
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

    /**
     * 创建流式文件推送会话。
     *
     * 与 [LocalDevice.pushFile] 不同，本方法不会在代理设备上生成完整临时文件，调用者可以直接将接收到的数据写入设备端。
     *
     * @param[sn] 设备序列号。
     * @param[remote] 目标路径（Android 端）。如果以 `/` 结尾则表示将文件写入该目录并保持原名，否则表示写入路径指定的文件。
     * @param[name] 源文件名称。目标路径以 `/` 结尾时使用该名称拼接最终路径。
     * @return 流式文件推送会话。
     * @throws[IllegalArgumentException] 目标路径或文件名称为空。
     * @throws[IllegalStateException] 设备不存在或未就绪。
     * @throws[IOException] 目标目录无法创建或不可写入，或 ADB 进程启动失败。
     */
    @Throws(IOException::class)
    fun openFilePushStream(sn: String, remote: String, name: String): ADBFilePushStream {
        require(remote.isNotBlank()) { "Remote path is empty!" }
        require(name.isNotBlank()) { "File name is empty!" }

        val device = requireDevice(sn)
        check(device.isOnline()) { "Device not ready or not exist." }


        /* 解析目标路径 */
        val remotePath = if (remote.endsWith("/")) "$remote$name" else remote
        val separator = remotePath.lastIndexOf('/')
        if (separator < 0) {
            throw IllegalArgumentException("Remote path parent is empty! Path:[$remotePath]")
        }
        val remoteParent = if (separator == 0) "/" else remotePath.substring(0, separator)


        /* 创建目标目录并检查可写性 */
        device.runForStatus("mkdir -p ${quote(remoteParent)}")
        if (device.runForStatus("test -w ${quote(remoteParent)}") != 0) {
            throw IOException("Remote directory cannot be write! Path:[$remoteParent]")
        }


        /* 建立设备端写入流 */
        val process = device.run("cat > ${quote(remotePath)}", redirectError = true)
        logger.debug("Create file push stream. SN:[{}] Remote:[{}]", sn, remotePath)
        return ADBFilePushStream(process, remotePath)
    }

    private fun requireDevice(sn: String): LocalDevice {
        return ADBController.getDevice(sn) ?: throw IllegalStateException("Device not found for sn: $sn")
    }

    /**
     * 为 Shell 参数添加单引号。
     *
     * @param[value] 原始文本。
     * @return 转义后的文本。
     */
    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
