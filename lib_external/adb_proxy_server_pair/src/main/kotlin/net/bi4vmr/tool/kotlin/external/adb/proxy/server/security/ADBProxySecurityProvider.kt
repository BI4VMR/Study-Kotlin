package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import io.ktor.server.application.Application
import java.security.KeyStore

/**
 * 安全模块的服务提供接口，通过 SPI 加载。
 *
 * 主模块的服务器引擎保持不变，加载安全模块后将监听协议由 HTTP 切换为 HTTPS，并安装认证与配对逻辑。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
interface ADBProxySecurityProvider {

    /**
     * 准备 TLS 凭据与认证逻辑。
     *
     * @param[config] 安全配置。
     */
    fun prepare(config: ADBProxySecurityConfig): SecureContext
}

/**
 * 安全上下文。
 *
 * @property[keyStore] 存放服务端证书的密钥库。
 * @property[keyAlias] 证书别名。
 * @property[keyStorePassword] 密钥库口令。
 * @property[privateKeyPassword] 私钥口令。
 * @property[fingerprint] 服务端证书的 SHA-256 指纹（十六进制）。
 * @property[install] 在应用中安装认证拦截器与配对路由，需在业务插件安装之后调用。
 */
class SecureContext(
    val keyStore: KeyStore,
    val keyAlias: String,
    val keyStorePassword: CharArray,
    val privateKeyPassword: CharArray,
    val fingerprint: String,
    val install: Application.() -> Unit
)
