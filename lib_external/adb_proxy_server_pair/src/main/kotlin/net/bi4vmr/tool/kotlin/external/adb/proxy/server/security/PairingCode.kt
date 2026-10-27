package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 配对码工具，用于识别中间人攻击。
 *
 * 流程：客户端先提交随机数 `cn` 的承诺，服务端返回随机数 `sn` ，客户端再公开 `cn` ；
 * 双方各自以「所见证书指纹 + cn + sn」计算 6 位配对码并展示，用户核对一致后在服务端批准。
 * 中间人使用的证书与真实服务端不同，且双方随机数都在对方承诺之后才确定，无法使两端配对码相同。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
object PairingCode {

    const val CODE_LENGTH: Int = 6

    private val random = SecureRandom()

    fun newNonce(): String = ByteArray(16).also { random.nextBytes(it) }.toHex()

    fun commit(clientNonce: String): String = sha256("commit".toByteArray() + clientNonce.toByteArray()).toHex()

    /**
     * 计算配对码。
     *
     * @param[fingerprint] 本端所见的服务端证书指纹（十六进制）。
     */
    fun compute(fingerprint: String, clientNonce: String, serverNonce: String): String {
        val hash = sha256("code|${fingerprint.lowercase()}|$clientNonce|$serverNonce".toByteArray())
        val value = hash.take(4).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
        return (value % 1_000_000L).toString().padStart(CODE_LENGTH, '0')
    }

    private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
