package net.bi4vmr.tool.kotlin.external.adb.cast.proxy.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.API
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.ADBCastStartRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyRouteProvider
import org.slf4j.LoggerFactory

/**
 * 服务端扩展：远程屏幕投射。
 */
class ADBCastRoutes : ADBProxyRouteProvider {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBCastRoutes::class.java)


    override fun Routing.register(jsonTool: Json) {
        post(API.ADB_CAST_START) {
            try {
                /* 参数校验 */
                val params: ADBCastStartRequest = call.receive()
                val device: LocalDevice? = ADBController.getDevices()
                    .firstOrNull { it.getSN() == params.sn && it.isOnline() }
                if (device == null) {
                    call.respondText("Device not ready or not exist.", status = HttpStatusCode.NotFound)
                    return@post
                }

                val port = ADBCastTransfer.start(device)
                call.respondText(port.toString(), ContentType.Text.Plain)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respondText("Failed to start cast.", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}
