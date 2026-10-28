package net.bi4vmr.tool.kotlin.external.adb.proxy.server.module

import io.ktor.server.application.*
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer

/**
 * 服务端模块。
 *
 * 可选模块需要实现该接口，并通过 SPI 机制加载。
 */
interface ADBProxyModule {

    /**
     * 注册模块。
     *
     * @param[server] 服务端实例，子模块可以复用 JSON 解析工具、协程作用域等。
     */
    fun Application.register(server: ADBProxyServer)

    /**
     * 注销模块。
     *
     * 服务端关闭时回调，子模块可在此释放资源。
     */
    fun unregister() {
        // 默认不执行任何操作。
    }
}
