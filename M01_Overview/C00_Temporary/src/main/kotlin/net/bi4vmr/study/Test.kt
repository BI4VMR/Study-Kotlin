package net.bi4vmr.study

import net.bi4vmr.tool.kotlin.external.adb.ADBCastEventListener
import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

fun main() {
    // 获取屏幕分辨率
    // val i = Toolkit.getDefaultToolkit().screenResolution
    // println(i)

    var line: SourceDataLine? = null

    ADBController.init()

    ADBCastManager.setServerFilePath("C:/Users/bi4vmr/Download/scrcpy-server-v4.1")

    ADBController.getDevices()
        .firstOrNull()
        ?.let {
            println("D -> $it")
            ADBCastManager.start(
                it, object : ADBCastEventListener {
                    override fun onVideoSizeChange(width: Int, height: Int) {
                        println("onSizeChange: width=$width, height=$height")
                    }

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
                        // TODO("Not yet implemented")
                    }

                    override fun onVideoError(error: Exception) {
                        println("onVideoError. ${error.message}")
                    }

                    override fun onVideoReady() {
                        println("onVideoReady")
                    }

                    override fun onAudioData(pcmData: ByteArray, channels: Int, sampleRate: Int) {
                        println("onAudioData -> ${pcmData.size}")
                        line ?: run {
                            val format = AudioFormat(sampleRate.toFloat(), 16, channels, true, false)
                            line = AudioSystem.getSourceDataLine(format).apply {
                                open(format)
                                start()
                            }
                        }
                        line?.write(pcmData, 0, pcmData.size)
                    }

                    override fun onAudioError(error: Exception) {
                        println("onAudioError. ${error.message}")
                    }
                }
            )
        }
        ?: println("No devices found")
}
