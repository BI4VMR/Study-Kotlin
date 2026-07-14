package net.bi4vmr.study

import net.bi4vmr.tool.java.external.adb.ADBController

/**
 * TODO 添加描述。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    // val i = Toolkit.getDefaultToolkit().screenResolution
    // println(i)
    println("-----init")
    ADBController.init()
    ADBController.getDevices().forEach {
        println(it)
    }
    ADBController.terminate()
}
