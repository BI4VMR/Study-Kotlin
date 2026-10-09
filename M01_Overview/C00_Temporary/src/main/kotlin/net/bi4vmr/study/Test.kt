package net.bi4vmr.study

import com.sun.jndi.toolkit.url.Uri
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import net.bi4vmr.tool.kotlin.external.adb.ADBCastEventListener
// import net.bi4vmr.tool.kotlin.external.adb.ADBCastManager
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import java.awt.Desktop
import java.awt.Toolkit
import java.io.FileReader
import java.io.FileWriter
import java.net.URI
import java.util.Properties
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

fun main() {
    // embeddedServer(CIO, port = 8080) {
    //     routing {
    //         get("/") {
    //             call.respondText("Hello Ktor")
    //         }
    //     }
    // }.start(wait = true)

    // 键值对
    // // 创建并从文件加载数据
    // Properties().load(FileReader(""))
    // // 将数据保存到流
    // Properties().store(FileWriter(""),"111")
    //
    // // 读程序内置的 src/main/resources/1.properties
    // // Properties().load(xxx.class.getClassLoader.getResourceAsStream("1.properties"))
    //
    // // 默认编码  ISO-8859-1 ，不支持非 ASCII 字符，中文会乱码，若要支持中文需在stream中指定utf-8编码
    // // 读写
    // Properties().getProperty("key")
    // Properties().getProperty("key","def")
    // Properties().setProperty("key","val")
    // // 所有属性集合
    // Properties().stringPropertyNames()

    /*
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
     */
}
