package net.bi4vmr.study

import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.ADBCastEventListener
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager

fun main() {
    // val i = Toolkit.getDefaultToolkit().screenResolution
    // println(i)

    ADBController.init(ADBController.findADBInPath()!!.absolutePath)

    ADBCastManager.setServerFilePath("C:/Users/bi4vmr/Download/scrcpy-server-v4.1")
    ADBCastManager.setServerVersion("4.1")

    ADBController.getDevices()
        .firstOrNull()
        ?.let {
            println("D -> $it")
            ADBCastManager.start(it, object : ADBCastEventListener {
                override fun onSizeChange(width: Int, height: Int) {
                    println("onSizeChange: width=$width, height=$height")
                }

                override fun onNewFrame(
                    yData: ByteArray,
                    yStride: Int,
                    uData: ByteArray,
                    uStride: Int,
                    vData: ByteArray,
                    vStride: Int
                ) {
                    // TODO("Not yet implemented")
                }

                override fun onAudioData(pcmData: ByteArray, channels: Int, sampleRate: Int) {

                }

                override fun onError(error: Exception) {
                    error.printStackTrace()
                }
            })
        }
}
