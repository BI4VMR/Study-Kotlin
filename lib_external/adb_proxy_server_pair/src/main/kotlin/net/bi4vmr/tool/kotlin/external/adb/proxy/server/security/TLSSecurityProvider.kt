package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import io.ktor.network.tls.certificates.generateCertificate
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.routes.installSecurity
import java.security.KeyStore

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
            install = { installSecurity(security) }
        )
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
