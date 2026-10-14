package net.bi4vmr.tool.kotlin.external.adb.cast

import net.bi4vmr.tool.kotlin.external.adb.ADBController

/**
 * 发送端启动入口：持续监听并转发设备数据，按回车键退出。
 *
 * 参数： `<scrcpy服务端JAR路径> [audio]` ，包含 `audio` 时启用音频。
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Usage: <scrcpy-server-path> [audio]" }
    val enableAudio = args.getOrNull(1) == "audio"

    ADBCastTransfer.setServerFilePath(args[0])
    if (!ADBController.isInitialized()) {
        ADBController.init()
    }
    val device = ADBController.getDevices().firstOrNull() ?: error("No ADB device connected!")

    val port = ADBCastTransfer.start(device, enableAudio = enableAudio)
    println("Transfer listening on port [$port], audio=[$enableAudio]. Press Enter to stop.")

    // 标准输入不可用（EOF）时持续阻塞，避免进程直接退出
    if (readlnOrNull() == null) Thread.currentThread().join()
    ADBCastTransfer.stop(port)
}
