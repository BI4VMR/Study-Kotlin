@file:OptIn(ExperimentalUuidApi::class)

package net.bi4vmr.tool.kotlin.external.adb.proxy.common.util

import net.bi4vmr.tool.java.security.digest.MessageDigestUtil
import java.security.cert.Certificate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 配对模块：通用工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object PairUtil {

    /**
     * 配对码长度。
     */
    const val CODE_LENGTH: Int = 6

    /**
     * 生成配对随机数。
     *
     * 每次配对都使用不同的随机数，可以防止重放攻击。
     *
     * @return 随机数。
     */
    fun generateNonce(): String = Uuid.generateV4().toHexString().uppercase()

    /**
     * 计算证书指纹。
     *
     * @param[cert] 证书。
     * @return SHA-256 摘要的十六进制小写字符串。
     */
    fun calculateCertFingerprint(cert: Certificate): String =
        MessageDigestUtil.getSHA256(cert.encoded)

    /**
     * 计算承诺值。
     */
    fun calculateCommit(fingerprint: String, nonce: String): String =
        MessageDigestUtil.getSHA256("pair-commit|${fingerprint.lowercase()}_${nonce.lowercase()}")

    /**
     * 计算配对码。
     *
     * @param[cfingerprint] 客户端证书指纹。
     * @param[sfingerprint] 服务端证书指纹。
     * @param[cNonce] 客户端随机数。
     * @param[sNonce] 服务端随机数。
     */
    fun calculatePairCode(cfingerprint: String, sfingerprint: String, cNonce: String, sNonce: String): String {

        /*
         * 使用域前缀 `pair` 与其他元素组合生成字符串。
         *
         * 域前缀用于区分不同的业务，防止攻击者从其他使用相同算法的接口获得关键因素后攻击当前业务接口。
         */
        val text =
            "pair|${cfingerprint.lowercase()}_${sfingerprint.lowercase()}_${cNonce.lowercase()}_${sNonce.lowercase()}"
        val hash = MessageDigestUtil.getSHA256(text)
        val value = hash.take(4).fold(0L) { sum, i ->
            // 每轮循环将当前累加数值的位左移 1 字节，空出低 8 位；然后获得当前字节的无符号值放入低 8 位。
            (sum shl 8) or (i.code.toLong() and 0xFF)
        }
        // 取余作为配对码，如果不满预定位数则在前部补零。
        return (value % 1_000_000L).toString()
            .padStart(CODE_LENGTH, '0')
    }
}
