package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * 服务端路由注册接口.
 */
interface ADBProxyRouteProvider {

    /**
     * 注册路由。
     *
     * @param[jsonTool] 全局 JSON 解析工具，可以按需使用。
     */
    fun Routing.register(jsonTool: Json)
}
