package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * 服务端安全策略的运行时逻辑：令牌签发与校验。
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

        fun generateToken(): String {
            val bytes = ByteArray(32).also { random.nextBytes(it) }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        fun hashToken(token: String): String =
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).toHex()

        fun fingerprintOf(keyStore: KeyStore): String =
            MessageDigest.getInstance("SHA-256")
                .digest(keyStore.getCertificate(KEY_ALIAS).encoded)
                .toHex()

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }

    val store get() = config.store

    fun isTokenValid(token: String?): Boolean {
        if (config.trustAllClients) return true
        if (token.isNullOrEmpty()) return false

        val hash = hashToken(token).toByteArray()
        // 恒定时间比较，并遍历全部记录，避免时序差异。
        var matched = false
        for (client in store.list()) {
            if (MessageDigest.isEqual(hash, client.tokenHash.toByteArray())) matched = true
        }
        return matched
    }
}
