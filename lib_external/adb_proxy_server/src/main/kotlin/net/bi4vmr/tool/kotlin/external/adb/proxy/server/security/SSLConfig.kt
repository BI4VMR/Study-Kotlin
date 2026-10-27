package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import net.bi4vmr.tool.java.common.base.FileUtil
import java.io.File
import java.security.KeyStore

/**
 * SSL 配置。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class SSLConfig {

    var keyStorePath: String = FileUtil.PATH_INVALID

    var keyStorePassword: String = ""

    var keyAlias: String = ""

    var keyPassword: String = ""


    fun getKeyStore(): KeyStore =
        KeyStore.getInstance(File(keyStorePath), keyStorePassword.toCharArray())
}
