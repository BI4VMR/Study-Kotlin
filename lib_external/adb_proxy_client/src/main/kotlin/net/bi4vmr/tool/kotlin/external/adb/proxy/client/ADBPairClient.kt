package net.bi4vmr.tool.kotlin.external.adb.proxy.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.tls.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.util.PairUtil
import org.slf4j.LoggerFactory

/**
 * ADB 代理配对客户端。
 *
 * 为了验证服务端身份，需要通过证书管理器的 `checkServerTrusted()` 回调获取服务端证书，但 [ADBProxyClient] 的证书管理器可能在配对过程中收到
 * 其他客户端的请求，导致证书管理混乱。为了避免上述情况，配对相关流程需要通过本类调用，与 [ADBProxyClient] 隔离。
 *
 * 该实例仅供配对过程中单次使用，若重新发起配对需要创建新的实例。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBPairClient(

    /**
     * 目标主机。
     */
    private val host: String,

    /**
     * 目标端口。
     *
     * 默认值为 [Constants.PORT_SERVER_DEFAULT]。
     */
    private val port: Int = Constants.PORT_SERVER_DEFAULT,

    /**
     * SSL 配置。
     */
    private val sslConfig: SSLConfig
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBPairClient::class.java)

    /**
     * 全局 JSON 工具。
     */
    private val jsonTool: Json = Json {
        // 解码时忽略未知字段
        ignoreUnknownKeys = true
        // 编码时对于未指明的字段填入默认值
        encodeDefaults = true
    }

    /**
     * SSL 证书管理器。
     */
    private val pairTrustManager = PairTrustManager()

    /**
     * Ktor 客户端。
     */
    private val client = HttpClient(CIO) {
        engine {
            https {
                trustManager = pairTrustManager

                try {
                    // 向服务端出示客户端证书
                    addKeyStore(sslConfig.getKeyStore(), sslConfig.keyPassword.toCharArray(), sslConfig.keyAlias)
                } catch (e: Exception) {
                    logger.warn("Failed to add keystore for client certificate. Reason:[${e.message}]")
                    e.printStackTrace()
                }
            }
        }
        install(HttpTimeout)
        // 日志输出
        install(Logging) { level = LogLevel.INFO }
        // 自动反序列化
        install(ContentNegotiation) { json(jsonTool) }
        // WebSocket 支持
        install(WebSockets)
    }


    /* ----- 服务状态 ----- */

    /**
     * 释放资源。
     *
     * 本方法调用后实例不再可用，若需重新配对，请创建新的实例。
     */
    fun release() {
        client.close()
    }


    /* ----- 配对流程 ----- */

    /**
     * 请求配对。
     *
     * 流程中会计算 6 位配对码并通过 [onCode] 通知上层展示，服务端也会展示同样的配对码，
     * 用户核对一致后在服务端批准；若两端不一致，说明通信可能被中间人劫持，应拒绝。
     * 配对码就绪后调用将被挂起，直至服务端用户操作或超时。
     *
     * 服务端以 TLS 握手中的客户端证书识别身份，配对成功后无需令牌。
     *
     * 配对过程中取消协程、配对码不一致或发生异常时，本方法会主动通知服务端释放会话；
     * 配对结束（成功或失败）后，调用方应调用 [release] 释放客户端实例。
     *
     * @param[onCode] 配对码就绪的回调。
     * @throws[IllegalStateException] 配对被拒绝、取消、超时或服务端繁忙。
     */
    suspend fun pair(onCode: (String) -> Unit = {}) {
        val clientNonce = PairUtil.generateNonce()
        val clientFingerprint = PairUtil.calculateCertFingerprint(sslConfig.requireCertificate())
        val clientCommit = PairUtil.calculateCommit(clientFingerprint, clientNonce)
        val commitRequest = PairCommitRequest(clientCommit)

        // 服务端是否仍持有本端的配对会话，结束或异常时需要主动通知服务端释放。
        var sessionPending = false

        try {
            /*
             * 第一阶段
             *
             * 双方交换随机数和证书的消息摘要，防止中间人篡改数据攻击。
             */
            val commitCallResponse = client.post(buildURL(APIs.PAIR_COMMIT)) {
                contentType(ContentType.Application.Json)
                setBody(commitRequest)
            }

            check(commitCallResponse.status.isSuccess()) {
                "Pairing failed. Status:[${commitCallResponse.status}] Reason:[${commitCallResponse.bodyAsText()}]"
            }

            val commitResponse = commitCallResponse.body<PairCommitResponse>()
            sessionPending = true

            /*
             * 第二阶段
             *
             * 双方交换随机数和证书，检查它们是否与第一阶段的消息摘要相符。
             */
            val revealCallResponse = client.post(buildURL(APIs.PAIR_REVEAL)) {
                contentType(ContentType.Application.Json)
                setBody(PairRevealRequest("TODO", "TODO", clientNonce))
            }

            check(revealCallResponse.status.isSuccess()) {
                "Pairing failed. Status:[${revealCallResponse.status}] Reason:[${revealCallResponse.bodyAsText()}]"
            }

            val revealResponse = revealCallResponse.body<PairRevealResponse>()

            val serverCertificate = pairTrustManager.requireServerCertificate()
            val serverFingerprint = PairUtil.calculateCertFingerprint(serverCertificate)

            // 检查服务端承诺值，确认服务端随机数在客户端提交随机数之前已经确定。
            val expected = PairUtil.calculateCommit(serverFingerprint, revealResponse.serverNonce)
            println("client -> sfp:[$serverFingerprint] sN:[$revealResponse]")
            if (expected != commitResponse.serverHash) {
                throw IllegalStateException("Server hash does not match.")
            }

            val code = PairUtil.calculatePairCode(
                clientFingerprint,
                serverFingerprint,
                clientNonce,
                revealResponse.serverNonce
            )

            // 先展示配对码，再等待服务端用户核对并批准。
            onCode(code)

            val confirmCallResponse = client.post(buildURL(APIs.PAIR_CONFIRM)) {
                // 服务端最多挂起 `PAIR_TIMEOUT_MS`，客户端略长，以便优先收到服务端返回的状态码。
                timeout { requestTimeoutMillis = Constants.PAIR_TIMEOUT_MS + 10_000L }
            }

            when (confirmCallResponse.status) {
                HttpStatusCode.OK -> {
                    sessionPending = false
                    logger.info("Pairing with [$host:$port] has been approved.")
                }

                HttpStatusCode.Forbidden -> {
                    sessionPending = false
                    throw IllegalStateException("Pairing was rejected by the server.")
                }

                HttpStatusCode.RequestTimeout -> {
                    sessionPending = false
                    throw IllegalStateException("Pairing timed out.")
                }

                HttpStatusCode.Conflict -> {
                    sessionPending = false
                    throw IllegalStateException("Pairing session is invalid or has been cancelled.")
                }

                else -> {
                    sessionPending = false
                    throw IllegalStateException(
                        "Pairing failed. Status:[${confirmCallResponse.status}] " +
                                "Reason:[${confirmCallResponse.bodyAsText()}]"
                    )
                }
            }
        } finally {
            // 用户取消、配对码不一致、协程被取消或中途异常时，告知服务端结束会话并关闭界面中的申请。
            // if (sessionPending) {
            //     withContext(NonCancellable) {
            //         runCatching { client.post(buildURL(APIs.PAIR_CANCEL)) }
            //             .onFailure { logger.warn("Failed to cancel pairing session. Reason:[${it.message}]") }
            //     }
            // }
        }
    }

    private fun buildURL(path: String): Url =
        URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = host,
            port = port
        )
            .apply {
                path(path)
            }
            .build()
}
