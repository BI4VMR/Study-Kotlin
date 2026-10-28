package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import io.ktor.network.tls.certificates.generateCertificate
import io.netty.channel.ChannelPipeline
import io.netty.handler.ssl.SslHandler
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * 安全模块入口：提供自签名证书与认证、配对逻辑，由主模块的服务器切换为 HTTPS 。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
class TLSSecurityProvider : ADBProxySecurityProvider {

    override fun prepare(config: ADBProxySecurityConfig): SecureContext {
        val keyStore = loadKeyStore(config)
        val security = ADBProxySecurity(config, ADBProxySecurity.fingerprintOf(keyStore))
        return SecureContext(
            keyStore = keyStore,
            keyAlias = ADBProxySecurity.KEY_ALIAS,
            keyStorePassword = ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray(),
            privateKeyPassword = ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray(),
            fingerprint = security.fingerprint,
            configurePipeline = { replaceSslHandler(keyStore) },
            install = {  }
        )
    }

    /**
     * 将 Ktor 创建的 TLS 处理器替换为“可选客户端证书”模式。
     *
     * Ktor 只要配置了信任库就强制要求客户端证书，且只信任库内证书，无法支持“先配对再信任”；
     * 因此这里接受任意客户端证书（握手时仍会验证对方持有私钥），是否信任由应用层按指纹判断。
     */
    private fun ChannelPipeline.replaceSslHandler(keyStore: KeyStore) {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray())

        val acceptAny = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS")
        context.init(kmf.keyManagers, arrayOf(acceptAny), null)

        val engine = context.createSSLEngine().apply {
            useClientMode = false
            wantClientAuth = true
        }
        replace("ssl", "ssl", SslHandler(engine))
    }

    private fun loadKeyStore(config: ADBProxySecurityConfig): KeyStore {
        val file = config.keyStoreFile
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            generateCertificate(
                file = file,
                algorithm = "SHA256withRSA",
                keyAlias = ADBProxySecurity.KEY_ALIAS,
                keyPassword = ADBProxySecurity.KEYSTORE_PASSWORD,
                jksPassword = ADBProxySecurity.KEYSTORE_PASSWORD,
                keySizeInBits = 2048
            )
        }
        return KeyStore.getInstance(file, ADBProxySecurity.KEYSTORE_PASSWORD.toCharArray())
    }
}
