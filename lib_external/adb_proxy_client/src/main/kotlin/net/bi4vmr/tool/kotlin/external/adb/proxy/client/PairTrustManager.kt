package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.util.PairUtil
import org.slf4j.LoggerFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * 配对证书管理器。
 *
 * 与服务端建立连接后保存其证书，外部可通过 [getServerCertificate] 方法获取该证书。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
internal class PairTrustManager : X509TrustManager {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBPairClient::class.java)

    /**
     * 首个连接的服务端证书。
     */
    private var serverCertificate: X509Certificate? = null

    /**
     * 首个连接是否已经到达。
     */
    private var firstConnectArrived: Boolean = false


    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // 此处为客户端，该方法不会被执行。
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // 服务端必须提供证书
        val cert: X509Certificate = chain?.firstOrNull() ?: throw CertificateException("Server certificate is missing!")

        synchronized(this) {
            if (!firstConnectArrived) {
                /* 仅接受首个连接的证书 */
                serverCertificate = cert
                firstConnectArrived = true
            } else {
                // 后续配对请求必须使用与首个连接相同的证书
                val firstFingerprint = serverCertificate?.let { PairUtil.calculateCertFingerprint(it) }
                val currentFingerprint = PairUtil.calculateCertFingerprint(cert)
                if (firstFingerprint != currentFingerprint) {
                    val info = "Server certificate mismatch!"
                    logger.error(info)
                    throw CertificateException(info)
                }
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    /**
     * 获取服务端证书。
     *
     * @return 服务端证书。若连接尚未建立则返回空值。
     */
    fun getServerCertificate(): X509Certificate? = serverCertificate

    /**
     * 获取服务端证书（非空）。
     *
     * 如果证书不存在则抛出异常，应当在连接建立后使用本方法。
     *
     * @throws[IllegalArgumentException] 证书不存在。
     */
    fun requireServerCertificate(): X509Certificate = requireNotNull(serverCertificate)
}
