package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRequest
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * 服务端安全配置。
 *
 * 所有通信均通过 TLS 加密；除配对申请外，其他接口都要求客户端携带配对时签发的令牌。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
class ADBProxySecurity(

    /**
     * 存放服务端自签名证书的密钥库文件，不存在时自动生成。
     */
    val keyStoreFile: File,

    /**
     * 已配对客户端存储。
     */
    val store: PairedClientStore = InMemoryPairedClientStore(),

    /**
     * 是否信任所有连接。
     *
     * 开启后跳过令牌校验与配对流程，仅用于调试，传输仍然加密。
     */
    @Volatile
    var trustAllClients: Boolean = false,

    /**
     * 配对审批回调：参数为申请信息与 6 位配对码，返回 `true` 表示批准配对。
     *
     * 界面需展示配对码，由用户与客户端展示的配对码核对一致后再批准。
     * 回调可以挂起以等待用户操作，超时时间为 [PAIR_TIMEOUT_MS] 。
     */
    val pairingApprover: suspend (PairRequest, String) -> Boolean = { _, _ -> false }
) {

    /**
     * 服务端证书指纹，由服务器启动时设置，用于计算配对码。
     */
    @Volatile
    var fingerprint: String = ""


    companion object {

        const val PAIR_TIMEOUT_MS: Long = 60_000

        // 密钥库仅用于存放自签名证书，口令不构成保护。
        internal const val KEYSTORE_PASSWORD = "androidtoolkit"
        internal const val KEY_ALIAS = "androidtoolkit"

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

    fun isTokenValid(token: String?): Boolean {
        if (trustAllClients) return true
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
