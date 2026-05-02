package net.bi4vmr.gradle.util

import java.net.InetSocketAddress
import java.net.Socket

/**
 * 网络相关工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object NetUtil {

    /**
     * 主机可达性侦测(TCP)。
     *
     * @param[host] 目标IP地址或域名。
     * @param[port] 目标端口。
     * @return `true` 表示目标可达； `false` 表示目标不可达。
     */
    fun scanByTCP(host: String, port: Int): Boolean {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(host, port), 500)
            LogUtil.info("TCP connect test success! Host:[$host] Port:[$port]")
            return true
        } catch (e: Exception) {
            LogUtil.error("TCP connect test failed! Host:[$host] Port:[$port] Reason:[${e.message}]")
            return false
        } finally {
            socket.close()
        }
    }
}
