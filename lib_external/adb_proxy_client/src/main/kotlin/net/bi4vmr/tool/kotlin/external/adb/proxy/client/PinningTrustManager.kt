package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import net.bi4vmr.tool.kotlin.external.adb.proxy.util.PairUtil
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * 基于证书指纹的信任管理器。
 *
 * 服务端使用自签名证书，因此不依赖 CA 链，而是核对叶子证书的 SHA-256 指纹：
 * - 已有固定指纹：必须一致，否则拒绝连接。
 * - 没有固定指纹（首次连接）：接受第一次看到的证书并记录到 [observedFingerprint] ，
 *   同一实例的后续连接必须与之一致；该指纹由配对码流程验证，通过后再由上层持久化。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
internal class PinningTrustManager(
    private val pinnedFingerprint: String?
) : X509TrustManager {

    @Volatile
    var observedFingerprint: String? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client authentication is not supported.")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("Empty certificate chain.")
        val fingerprint = PairUtil.calculateCertFingerprint(leaf)

        val expected = pinnedFingerprint ?: observedFingerprint
        if (expected != null && !expected.equals(fingerprint, ignoreCase = true)) {
            throw CertificateException("Server certificate fingerprint mismatch: $fingerprint")
        }
        observedFingerprint = fingerprint
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
