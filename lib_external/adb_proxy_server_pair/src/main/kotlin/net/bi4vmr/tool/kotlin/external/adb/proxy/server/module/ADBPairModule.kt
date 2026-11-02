package net.bi4vmr.tool.kotlin.external.adb.proxy.server.module

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.netty.NettyApplicationCall
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.netty.handler.ssl.SslHandler
import kotlinx.coroutines.CancellationException
import net.bi4vmr.tool.java.security.digest.MessageDigestUtil
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.APIs
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.ADBPairService
import org.slf4j.LoggerFactory
import java.security.cert.Certificate

class ADBPairModule : ADBProxyModule {

    companion object {

        /**
         * 公开路径。
         *
         * 无需校验客户端证书即可访问的接口。
         */
        private val pathsPublic = setOf(
            APIs.SERVER_INFO,
            APIs.PAIR_COMMIT,
            APIs.PAIR_REVEAL,
            APIs.PAIR_ASK,
            APIs.PAIR_CANCEL
        )
    }

    private lateinit var service: ADBPairService

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBPairModule::class.java)


    override fun Application.register(server: ADBProxyServer) {
        val runningConfig = server.getRunningConfig()
        val ssl = runningConfig?.sslConfig
        requireNotNull(ssl) { "SSL config not found! have you set `sslConfig` at first?" }

        service = ADBPairService(ssl, runningConfig.pairedPeerStore, runningConfig.pairRequestListener)

        // 注册拦截器，受控接口被访问时，检查客户端证书是否已与服务端配对。
        intercept(ApplicationCallPipeline.Plugins) {
            /* 不拦截公开路径 */
            val path = call.request.path()
            if (path in pathsPublic) return@intercept

            /* 检查客户端是否已出示证书 */
            val clientCert: Certificate? = call.getClientCertificate()
            if (clientCert == null) {
                call.respondText("Client certificate is required!", status = HttpStatusCode.BadRequest)
                finish()
                return@intercept
            }

            /* 检查证书是否已存在配对记录 */
            val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
            // TODO
            println("onInterceptCall -> [$fingerprint]")
            val knowns = server.getRunningConfig()?.pairedPeerStore!!
            val known = knowns.onList().any { it.fingerprint == fingerprint }
            if (!known) {
                call.respondText("Client certificate is not paired!", status = HttpStatusCode.Unauthorized)
                finish()
            }
        }

        routing {
            post(APIs.PAIR_COMMIT) {
                val clientCert = call.getClientCertificate()
                if (clientCert == null) {
                    call.respondText("Client certificate is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
                val body: PairCommitRequest = try {
                    call.receive()
                } catch (_: Exception) {
                    call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                try {
                    val response = service.onCommit(fingerprint, body)
                    call.respond(response)
                } catch (e: Exception) {
                    logger.error("Error occurred during pairing commit for client [$fingerprint]: ${e.message}", e)
                    call.respondText("Error occurred: ${e.message}", status = HttpStatusCode.BadRequest)
                }
            }

            post(APIs.PAIR_REVEAL) {
                val clientCert = call.getClientCertificate()
                if (clientCert == null) {
                    call.respondText("Client certificate is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
                val body: PairRevealRequest = try {
                    call.receive()
                } catch (_: Exception) {
                    call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                try {
                    val response = service.onReveal(fingerprint, body)
                    call.respond(response)
                } catch (e: Exception) {
                    logger.error("Error occurred during pairing reveal for client [$fingerprint]: ${e.message}", e)
                    call.respondText("Error occurred: ${e.message}", status = HttpStatusCode.BadRequest)
                }
            }

            post(APIs.PAIR_ASK) {
                val clientCert = call.getClientCertificate()
                if (clientCert == null) {
                    call.respondText("Client certificate is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
                val result = try {
                    // 挂起，直至服务端用户确认/拒绝、客户端取消或会话超时。
                    service.onAsk(fingerprint)
                } catch (e: CancellationException) {
                    // 客户端断开或服务端停止时，保持取消语义，交由 Ktor 处理。
                    throw e
                } catch (e: IllegalStateException) {
                    logger.warn("Invalid pairing confirm for client [$fingerprint]: ${e.message}")
                    call.respondText(
                        "Pairing session is invalid or has expired.",
                        status = HttpStatusCode.Conflict
                    )
                    return@post
                }

                println("onConfirm -> [$fingerprint] decision: [$result]")

                call.respondText("${result.code}")
            }

            post(APIs.PAIR_CANCEL) {
                val clientCert = call.getClientCertificate()
                if (clientCert == null) {
                    call.respondText("Client certificate is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
                service.onCancel(fingerprint)

                call.respond(HttpStatusCode.OK)
            }
        }
    }

    override fun unregister() {
        if (::service.isInitialized) {
            service.release()
        }
    }

    /**
     * 获取客户端证书。
     *
     * Ktor 未提供 SSL 证书相关接口，因此主模块使用 Netty 作为引擎，此处获取 Netty 内置证书管理对象并获取客户端证书。
     *
     * 该扩展方法作用于 [ApplicationCall] ，可在拦截器中使用，详见下文示例代码：
     *
     * ```kotlin
     * intercept(ApplicationCallPipeline.Plugins) {
     *     val clientCert = call.getClientCertificate()
     * }
     *
     * @return 客户端证书，如果客户端未配置证书则返回空值。
     */
    private fun ApplicationCall.getClientCertificate(): Certificate? {
        val sslHandler = (this as? NettyApplicationCall)?.context?.pipeline()?.get(SslHandler::class.java)
        val chain = sslHandler?.engine()?.session?.peerCertificates
        return chain?.firstOrNull()
    }

    private fun ApplicationCall.requireClientCertificate(): Certificate {
        return this.getClientCertificate() ?: throw IllegalArgumentException("Client certificate is required!")
    }

    /**
     * 获取客户端证书。
     *
     * Ktor 未提供 SSL 证书相关接口，因此主模块使用 Netty 作为引擎，此处获取 Netty 内置证书管理对象并获取客户端证书。
     *
     * 该扩展方法作用于 [RoutingCall] ，可在路由处理器中使用，详见下文示例代码：
     *
     * ```kotlin
     * routing {
     *     post("/example") {
     *         val clientCert = call.getClientCertificate()
     *     }
     * }
     *
     * @return 客户端证书，如果客户端未配置证书则返回空值。
     */
    private fun RoutingCall.getClientCertificate(): Certificate? {
        val engineCall = this.pipelineCall.engineCall
        val sslHandler = (engineCall as? NettyApplicationCall)?.context?.pipeline()?.get(SslHandler::class.java)
        val chain = sslHandler?.engine()?.session?.peerCertificates
        return chain?.firstOrNull()
    }

    private fun RoutingCall.requireClientCertificate(): Certificate {
        return this.getClientCertificate() ?: throw IllegalArgumentException("Client certificate is required!")
    }
}
