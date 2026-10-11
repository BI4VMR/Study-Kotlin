package net.bi4vmr.study

import net.bi4vmr.tool.kotlin.external.adb.cast.common.ADBCastEventListener
import net.bi4vmr.tool.kotlin.external.adb.ADBRemoteCastManager
import net.bi4vmr.tool.kotlin.external.adb.cast.common.control.InputController
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * 远程投屏测试：接收端。
 *
 * 请先运行本程序监听端口，再运行 [RemoteCastSenderTest] 。
 */
fun main() {
    var line: SourceDataLine? = null

    val listener = object : ADBCastEventListener {
        override fun onVideoSizeChange(width: Int, height: Int) {
            println("onVideoSizeChange: width=$width, height=$height")
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
            // 仅验证数据到达，不做渲染。
        }

        override fun onVideoReady() {
            println("onVideoReady")
        }

        override fun onVideoError(error: Exception) {
            println("onVideoError. ${error.message}")
        }

        override fun onTitleResolve(title: String) {
            println("onTitleResolve: $title")
        }

        override fun onControlReady(controller: InputController) {
            println("onControlReady")
        }

        override fun onAudioData(pcmData: ByteArray, channels: Int, sampleRate: Int) {
            val current = line ?: run {
                val format = AudioFormat(sampleRate.toFloat(), 16, channels, true, false)
                AudioSystem.getSourceDataLine(format).apply {
                    open(format)
                    start()
                }.also { line = it }
            }
            current.write(pcmData, 0, pcmData.size)
        }

        override fun onAudioError(error: Exception) {
            println("onAudioError. ${error.message}")
        }
    }

    val port = ADBRemoteCastManager.startListening("0.0.0.0", 18080, listener)
    if (port < 0) {
        println("Listen failed!")
        return
    }

    println("Listening at port [$port]. Press Enter to stop.")
    readlnOrNull()
    ADBRemoteCastManager.stop(listener)
    line?.close()
}
