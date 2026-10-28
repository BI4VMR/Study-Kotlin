package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import net.bi4vmr.tool.kotlin.external.adb.proxy.util.PairUtil

/**
 * 服务端安全策略的运行时逻辑：基于客户端证书指纹的认证。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
internal class ADBProxySecurity(
    val config: ADBProxySecurityConfig,

    /**
     * 服务端证书指纹，用于计算配对码。
     */
    val fingerprint: String
) {

    companion object {

        const val PAIR_TIMEOUT_MS: Long = 60_000

        // 密钥库仅用于存放自签名证书，口令不构成保护。
        const val KEYSTORE_PASSWORD = "androidtoolkit"
        const val KEY_ALIAS = "androidtoolkit"

        private val random = SecureRandom()

        fun generateSessionID(): String {
            val bytes = ByteArray(32).also { random.nextBytes(it) }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        fun fingerprintOf(keyStore: KeyStore): String =
            PairUtil.getFingerprint(keyStore.getCertificate(KEY_ALIAS))
    }

    val store get() = config.store

    /**
     * 检查客户端是否已配对。
     *
     * @param[fingerprint] TLS 握手中客户端证书的指纹，没有出示证书时为空值。
     */
    fun isPaired(fingerprint: String?): Boolean {
        if (config.trustAllClients) return true
        if (fingerprint == null) return false
        return store.list().any { it.fingerprint == fingerprint }
    }
}
