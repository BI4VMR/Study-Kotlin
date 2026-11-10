package net.bi4vmr.study

import net.bi4vmr.tool.java.common.base.file.FilePathUtil
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun main() {
    // Uuid.generateV4().toHexString().uppercase().let {
    //     println(it)
    // }
    //
    // TerminalUtil.launch(
    //     terminal = Terminals.CMD,
    //     command = "ps",
    //     newWindow = false,
    //     keep = true
    // )

    ADBExecutableHelper.setExecutablePath("/home/bi4vmr/Software/Android-SDK/platform-tools/adb")
    ADBController.init()
    ADBController.getDevices().firstOrNull()?.let {
        println(it.getSN())
        it.pushFile(
            File("/home/bi4vmr/Downloads/jadx-1.5.2.zip"),
            "/sdcard/Download/abc/",
            object : ADBFileTransferListener() {
                override fun onStart(size: Long) {
                    println("onStart size -> $size")
                }

                override fun onProgress(size: Long, progress: Double) {
                    println("onProgress size -> $size, progress -> $progress")
                }

                override fun onEnd(cancel: Boolean) {
                    println("onEnd cancel -> $cancel")
                }
            }
        )
    }

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
}
