package net.bi4vmr.study

import net.bi4vmr.tool.kotlin.external.adb.cast.common.ADBCastEventListener
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ADBRemoteCastManager

/**
 * 远程投屏测试：发送端。
 *
 * 请先运行 [RemoteCastReceiverTest] 监听端口，再运行本程序。
 */
fun main() {
    ADBController.init()
    ADBRemoteCastManager.setServerFilePath("C:/Users/bi4vmr/Download/scrcpy-server-v4.1")

    val device = ADBController.getDevices().firstOrNull()
    if (device == null) {
        println("No devices found")
        return
    }
    println("D -> $device")

    // 发送端不处理画面与声音，仅关注错误回调。
    val listener = object : ADBCastEventListener {
        override fun onVideoSizeChange(width: Int, height: Int) {}

        override fun onVideoFrame(
            yData: ByteArray,
            yStride: Int,
            uData: ByteArray,
            uStride: Int,
            vData: ByteArray,
            vStride: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun onVideoError(error: Exception) {
            println("onVideoError. ${error.message}")
        }
    }

    ADBRemoteCastManager.startForward("127.0.0.1", 18080, device.getSN(), listener)

    println("Forwarding. Press Enter to stop.")
    readlnOrNull()
    ADBRemoteCastManager.stop(listener)
}
