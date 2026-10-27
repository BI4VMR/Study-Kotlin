package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRequest
import java.io.File

/**
 * 服务端安全配置。
 *
 * 传入 [net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer.setSecurity] 后，服务端将启用 HTTPS 与配对机制，
 * 该能力由独立的安全模块（`adb_proxy_server_security`）提供，运行时必须存在于类路径中。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
class ADBProxySecurityConfig(

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
     * 开启后跳过令牌校验与配对流程，仅用于调试，传输仍然加密；即时生效。
     */
    @Volatile
    var trustAllClients: Boolean = false,

    /**
     * 配对审批回调：参数为申请信息与 6 位配对码，返回 `true` 表示批准配对。
     *
     * 界面需展示配对码，由用户与客户端展示的配对码核对一致后再批准。
     * 回调可以挂起以等待用户操作，超时时间为 60 秒。
     */
    val pairingApprover: suspend (PairRequest, String) -> Boolean = { _, _ -> false }
)
