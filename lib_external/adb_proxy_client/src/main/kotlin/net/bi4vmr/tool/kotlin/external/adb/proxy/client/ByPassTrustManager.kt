package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * 默认信任证书管理器。
 *
 * 信任所有服务端证书，由应用层自行执行鉴权，通常用于 P2P 等无 CA 体系的环境。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
internal class ByPassTrustManager : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // 此处为客户端，该方法不会被执行。
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // 不抛出异常即表示证书检查通过。
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
