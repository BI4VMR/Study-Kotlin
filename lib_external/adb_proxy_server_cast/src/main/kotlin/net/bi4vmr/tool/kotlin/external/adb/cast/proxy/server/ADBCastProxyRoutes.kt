package net.bi4vmr.tool.kotlin.external.adb.cast.proxy.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastTransfer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyRouteProvider
import org.slf4j.LoggerFactory
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice as CastADBDevice

/**
 * 可选的 ADB 投屏代理路由。
 *
 * 该模块通过 Java SPI 加载，因此投屏功能不会成为代理服务器的强制依赖。
 */
class ADBCastProxyRoutes : ADBProxyRouteProvider {

    /**
     * 日志工具
     */
    private val logger = LoggerFactory.getLogger(ADBCastProxyRoutes::class.java)

    /**
     * 注册投屏路由。
     */
    override fun register(routing: Routing) {
        routing.post("/adb/cast") {
            val sn = call.request.queryParameters["sn"]
            if (sn.isNullOrBlank()) {
                call.respondText("Parameter [sn] is required.", status = HttpStatusCode.BadRequest)
                return@post
            }

            val device = ADBController.getDevices().firstOrNull { it.getSN() == sn && it.isOnline() }
            if (device == null) {
                call.respondText("Device not ready or not exist.", status = HttpStatusCode.NotFound)
                return@post
            }

            try {
                val castDevice = CastADBDevice(device.ddmDevice.state, device.ddmDevice)
                val port = ADBCastTransfer.start(castDevice)
                call.respondText(port.toString(), ContentType.Text.Plain)
            } catch (e: Exception) {
                logger.error("Failed to start cast for device [$sn].", e)
                call.respondText("Failed to start cast.", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}
