package net.bi4vmr.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient

/**
 * 示例： ADB 屏幕投射控件。
 *
 * 调试方法：
 *
 * `./gradlew :demo_external:adb_proxy_client:run --console=plain`
 */
fun main() = application {
    // 创建客户端实例，配置服务端地址和端口。
    val client = ADBProxyClient("127.0.0.1")

    // 注册设备列表监听器，监听设备列表变化。
    CoroutineScope(Dispatchers.IO).launch {
        client.devicesFlow.collect { devices ->
            println("设备列表变化：$devices")
        }
    }

    val windowState = rememberWindowState()
    Window(
        title = "示例：ADB 代理客户端",
        state = windowState,
        onCloseRequest = ::exitApplication
    ) {
        Column {
            Button(onClick = {
                runBlocking {
                    runCatching {
                        val list = client.fetchDeviceList()
                        println("设备列表：$list")
                    }.onFailure {
                        println("获取设备列表失败：${it.message}")
                    }
                }
            }) {
                Text("获取设备列表")
            }

            Button(onClick = {
                client.startListenDeviceList()
            }) {
                Text("开始监听设备列表")
            }

            Button(onClick = {
                client.stopListenDeviceList()
            }) {
                Text("停止监听设备列表")
            }

            /*
             * 示例：远程 Shell ，通过 socat 接入系统原生终端。
             *
             * 必须由 `stty raw -echo` 将终端切换为 raw 模式，方向键、Tab 等控制字符才能原样传到设备。
             * 不要依赖 socat 的 `raw` 选项：socat 1.7.3.x 会将其误用于 TCP 连接，要么报 `tcsetattr ... Inappropriate ioctl for device` ，
             * 要么终端仍处于行缓冲模式，导致按上键显示 `^[[A` 。
             */
            Button(onClick = {
                runBlocking {
                    client.fetchDeviceList().firstOrNull()
                        ?.let {
                            val bridge = client.openShellBridge(it.sn)
                            bridge.use {
                                val script = "stty raw -echo; socat - TCP:127.0.0.1:${bridge.port}; stty sane"
                                val code = ProcessBuilder("konsole", "-e", "sh", "-c", script)
                                    .inheritIO()
                                    .start()
                                    .waitFor()
                                System.err.println("\n会话已结束（终端退出码 $code）")
                            }
                        }
                }
            }) {
                Text("远程 Shell")
            }
        }
    }
}
