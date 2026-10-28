package net.bi4vmr.tool.kotlin.external.adb.proxy.security

import net.bi4vmr.tool.java.common.base.FileUtil
import java.io.File
import java.security.KeyStore
import java.security.cert.Certificate

/**
 * SSL 配置。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class SSLConfig(

    val keyStoreFile: File = FileUtil.FILE_INVALID,

    val keyStorePassword: String = "",

    val keyAlias: String = "",

    val keyPassword: String = "",
) {

    fun getKeyStore(): KeyStore =
        KeyStore.getInstance(keyStoreFile, keyStorePassword.toCharArray())

    fun getCertificate(): Certificate? =
        getKeyStore().getCertificate(keyAlias)
}
