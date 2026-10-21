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
    client.init()

    // 注册设备列表监听器，监听设备列表变化。
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        client.serviceStateFlow.collect {
            println("连接状态变化：$it")
        }
    }
    scope.launch {
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
            //
            // Button(onClick = {
            //     client.startListenDeviceList()
            // }) {
            //     Text("开始监听设备列表")
            // }
            //
            // Button(onClick = {
            //     client.stopListenDeviceList()
            // }) {
            //     Text("停止监听设备列表")
            // }

            Button(onClick = {
                runBlocking {
                    client.fetchDeviceList().firstOrNull()
                        ?.let {
                            client.openShell(it)
                        }
                }
            }) {
                Text("远程 Shell")
            }

            val cmd = "free -h"

            Button(onClick = {
                runBlocking {
                    client.fetchDeviceList().firstOrNull()
                        ?.let {
                            val c = client.runForStatus(it.getSN(), cmd)
                            println("执行命令[$cmd]返回状态码：$c")
                        }

                }
            }) {
                Text("执行命令:cmd code")
            }

            Button(onClick = {
                runBlocking {
                    client.fetchDeviceList().firstOrNull()
                        ?.let {
                            val c = client.runForText(it.sn, cmd)
                            println("执行命令[$cmd]：")
                            println("$c")
                        }

                }
            }) {
                Text("执行命令:cmd text")
            }
        }
    }
}
