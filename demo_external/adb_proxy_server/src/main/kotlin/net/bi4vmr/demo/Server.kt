package net.bi4vmr.demo

import net.bi4vmr.tool.kotlin.external.adb.proxy.constant.Constant
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer

/**
 * 示例：服务端。
 *
 * 启动方式：
 *
 * `./gradlew :demo_external:adb_proxy_server:run --console=plain --args="5038"`
 *
 * 参数说明：
 *
 * - 第一参数为监听端口，默认值为 5038 。
 */
fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: Constant.PORT_SERVER_DEFAULT
    ADBProxyServer(port = port).start()
}
