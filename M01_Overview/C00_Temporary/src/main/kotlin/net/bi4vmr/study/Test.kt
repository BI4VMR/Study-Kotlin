package net.bi4vmr.study

import net.bi4vmr.tool.java.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastEventListener
import net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastManager
import net.bi4vmr.tool.kotlin.external.scrcpy.ui.ScreenCastWindow

/**
 * TODO 添加描述。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    println("ADB init")
    ADBController.init()
    println("ADB init done")
    ADBController.getDevices().firstOrNull()
        ?.let {
            println("ScrcpyUtil $it")

            ScreenCastManager.setServerFilePath("/home/bi4vmr/Work/scrcpy-server")
            ScreenCastManager.setServerVersion("4.0")

            // val i = object : ScreenCastEventListener {
            //     override fun onSizeChange(width: Int, height: Int) {
            //         println("onSizeChange: $width x $height")
            //     }
            //
            //     override fun onNewFrame(
            //         yData: ByteArray,
            //         yStride: Int,
            //         uData: ByteArray,
            //         uStride: Int,
            //         vData: ByteArray,
            //         vStride: Int
            //     ) {
            //         println("onNewFrame: yStride: $yStride uStride: $uStride vStride: $vStride")
            //     }
            //
            //     override fun onError(error: Exception) {
            //         println("onerror: $error : ${error.message}")
            //         error.printStackTrace()
            //     }
            //
            //     override fun onTitleResolve(title: String) {
            //         println("onTitleResolve: $title")
            //     }
            // }
            // ScreenCastManager.start(it, i)

            val window = ScreenCastWindow("My Device")
            window.connect(it)
        }

    Thread.sleep(99999L)
    println("full end")
    // ADBController.terminate()
}
