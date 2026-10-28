package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.netty.NettyApplicationCall
import io.netty.handler.ssl.SslHandler
import net.bi4vmr.tool.kotlin.external.adb.proxy.util.PairUtil
import java.security.cert.Certificate
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * 获取 TLS 握手中客户端出示的证书指纹。
 *
 * 客户端必须持有证书对应的私钥才能完成握手，因此该指纹可作为客户端身份。
 *
 * @return 指纹（十六进制小写）；客户端未出示证书或连接不是 Netty TLS 连接时返回空值。
 */
internal fun ApplicationCall.peerFingerprint(): String? {
    val ssl = (this as? NettyApplicationCall)?.context?.pipeline()?.get(SslHandler::class.java) ?: return null
    val chain: Array<Certificate> = try {
        ssl.engine().session.peerCertificates
    } catch (_: SSLPeerUnverifiedException) {
        return null
    }
    return chain.firstOrNull()?.let { PairUtil.getFingerprint(it) }
}
