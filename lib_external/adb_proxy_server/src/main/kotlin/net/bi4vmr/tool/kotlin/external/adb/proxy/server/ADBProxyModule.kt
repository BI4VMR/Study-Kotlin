package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.server.application.Application
import kotlinx.serialization.json.Json

/**
 * 服务端可选模块接口，通过 SPI 机制加载。
 *
 * 与 [net.bi4vmr.tool.kotlin.external.adb.proxy.server.routes.ADBProxyRouteProvider] 相比，
 * 该接口可以访问 [Application]，因此除了路由之外，还能安装插件、订阅事件等。
 */
interface ADBProxyModule {

    /**
     * 安装模块。
     *
     * @param[jsonTool] 全局 JSON 解析工具，可以按需使用。
     */
    fun Application.install(jsonTool: Json)
}
