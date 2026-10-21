package net.bi4vmr.tool.kotlin.external.adb.proxy.server

import io.ktor.server.routing.Routing

/**
 * Optional SPI for registering additional ADB proxy routes.
 */
interface ADBProxyRouteProvider {

    /**
     * Registers routes on the proxy server.
     */
    fun register(routing: Routing)
}
