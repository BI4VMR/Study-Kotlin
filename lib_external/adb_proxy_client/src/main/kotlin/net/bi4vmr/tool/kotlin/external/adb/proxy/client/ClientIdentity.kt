package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import io.ktor.network.tls.certificates.generateCertificate
import net.bi4vmr.tool.kotlin.external.adb.proxy.util.PairUtil
import java.io.File
import java.security.KeyStore

/**
 * 客户端身份：自签名证书与私钥。
 *
 * 连接服务端时通过 TLS 出示证书，服务端以证书指纹识别并授权客户端；
 * 私钥不会离开本机，因此他人无法冒用已配对的客户端。
 *
 * @property[keyStore] 存放证书与私钥的密钥库。
 * @property[fingerprint] 证书的 SHA-256 指纹（十六进制小写）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
class ClientIdentity private constructor(
    internal val keyStore: KeyStore,
    val fingerprint: String
) {

    companion object {

        internal const val PASSWORD = "androidtoolkit"
        internal const val ALIAS = "androidtoolkit-client"

        /**
         * 读取身份文件，文件不存在时生成新的身份并保存。
         *
         * 上层应将该文件持久化保存，更换文件意味着需要重新配对。
         */
        @JvmStatic
        fun loadOrCreate(file: File): ClientIdentity {
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                generateCertificate(
                    file = file,
                    algorithm = "SHA256withRSA",
                    keyAlias = ALIAS,
                    keyPassword = PASSWORD,
                    jksPassword = PASSWORD,
                    keySizeInBits = 2048
                )
            }
            val keyStore = KeyStore.getInstance(file, PASSWORD.toCharArray())
            return ClientIdentity(keyStore, "PairUtil.getFingerprint(keyStore.getCertificate(ALIAS))")
        }
    }
}
