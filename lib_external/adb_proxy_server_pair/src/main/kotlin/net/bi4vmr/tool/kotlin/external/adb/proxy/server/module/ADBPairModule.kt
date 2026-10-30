package net.bi4vmr.tool.kotlin.external.adb.proxy.server.module

import io.ktor.http.ContentType
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
import net.bi4vmr.tool.java.security.digest.MessageDigestUtil
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.API
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.service.ADBPairService
import org.slf4j.LoggerFactory
import java.security.cert.Certificate
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger("ADBSecurityRoutes")

private val ClientFingerprintKey = ADBPairService.ClientFingerprintKey

class ADBPairModule : ADBProxyModule {

    companion object {

        /**
         * 公开路径。
         *
         * 无需校验证书即可访问的接口。
         */
        private val pathsPublic = setOf(API.SERVER_VERSION, API.PAIR_COMMIT, API.PAIR_REVEAL)
    }

    private val service: ADBPairService = ADBPairService()

    private val logger = LoggerFactory.getLogger(ADBPairModule::class.java)


    // 同一时刻只允许一个配对会话，避免恶意客户端刷屏。
    val session = AtomicReference<PairSession?>(null)

    override fun Application.register(server: ADBProxyServer) {
        requireNotNull(server.getSSLConfig()) { "SSL configuration not found! have you call `setSSLConfig()` at first?" }

        // 注册拦截器，受控接口被访问时，检查客户端证书是否已与服务端配对。
        intercept(ApplicationCallPipeline.Plugins) {
            // 如果请求的路径是公开的，无需检查客户端证书。
            val path = call.request.path()
            if (path in pathsPublic) return@intercept

            val clientCert: Certificate? = call.getClientCertificate()
            if (clientCert == null) {
                call.respondText("Client certificate is required!", status = HttpStatusCode.Unauthorized)
                finish()
                return@intercept
            }

            val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
            println("onInterceptCall -> [$fingerprint]")
        }

        routing {
            post(API.PAIR_COMMIT) {
                val clientCert = call.getClientCertificate()
                if (clientCert == null) {
                    call.respondText("Client certificate is required!", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
                val body: PairCommitRequest = try {
                    call.receive()
                } catch (e: Exception) {
                    call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val response = service.onCommit(fingerprint, body)
                call.respond(response)

                // val clientFingerprint = call.attributes.getOrNull(ClientFingerprintKey) ?: return@post
                // val request: PairCommitRequest = try {
                //     call.receive()
                // } catch (e: Exception) {
                //     call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                //     return@post
                // }
                //
                // call.respond(PairCommitResponse("1", "serverNonce"))

                // if (request.clientName.length > 64 || request.commit.length != 64) {
                //     call.respondText("Invalid client information.", status = HttpStatusCode.BadRequest)
                //     return@post
                // }

                // val now = System.currentTimeMillis()
                // val created = PairSession(
                //     id = "1",
                //     clientFingerprint = clientFingerprint,
                //     serverNonce = PairUtil.generateNonce(),
                //     expireAt = now + 5000
                // )
                // val current = session.get()
                // val free = current == null || current.expireAt < now
                // if (!free || !session.compareAndSet(current, created)) {
                //     call.respondText("Another pairing request is pending.", status = HttpStatusCode.TooManyRequests)
                //     return@post
                // }
                // call.respond(PairCommitResponse(created.id, created.serverNonce))
            }

            post(API.PAIR_REVEAL) {
                val clientFingerprint = call.attributes.getOrNull(ClientFingerprintKey) ?: return@post
                val reveal: PairRevealRequest = try {
                    call.receive()
                } catch (e: Exception) {
                    call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                // 会话只能使用一次，无论成败都会被消耗，防止对承诺或配对码的重复尝试。
                val current = session.get()
                if (current == null || current.id != "reveal.pairId" || !session.compareAndSet(current, null)) {
                    call.respondText("Pairing session not found.", status = HttpStatusCode.NotFound)
                    return@post
                }
                // 两步必须使用同一张客户端证书。
                if (current.clientFingerprint != clientFingerprint) {
                    call.respondText("Client certificate mismatch.", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val remaining = current.expireAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    call.respondText("Pairing session expired.", status = HttpStatusCode.Gone)
                    return@post
                }
                // if (PairUtil.getCommit(reveal.clientNonce) != current.request.commit) {
                //     call.respondText("Commitment mismatch.", status = HttpStatusCode.BadRequest)
                //     return@post
                // }

                // val code = PairUtil.generatePairCode(
                //     clientFingerprint, security.fingerprint, reveal.clientNonce, current.serverNonce
                // )
                // val approval = PairApproval(current.request.clientName, clientFingerprint, code)
                // val approved = security.config.trustAllClients ||
                //         withTimeoutOrNull(remaining) { security.config.pairingApprover(approval) } == true
                // if (!approved) {
                //     logger.info("Pairing request from [{}] was rejected or timed out.", clientFingerprint)
                //     call.respondText("Pairing rejected.", status = HttpStatusCode.Forbidden)
                //     return@post
                // }

                // security.store.save(
                //     PairedClient(
                //         fingerprint = clientFingerprint,
                //         name = current.request.clientName,
                //         pairedAt = System.currentTimeMillis()
                //     )
                // )
                logger.info("Client [{}] has been paired.", clientFingerprint)
                call.respondText("OK", ContentType.Text.Plain)
            }
        }
    }

    private fun ApplicationCall.getClientCertificate(): Certificate? {
        val sslHandler = (this as? NettyApplicationCall)?.context?.pipeline()?.get(SslHandler::class.java)
        val chain = sslHandler?.engine()?.session?.peerCertificates
        return chain?.firstOrNull()
    }

    private fun ApplicationCall.requireClientCertificate(): Certificate {
        return this.getClientCertificate() ?: throw IllegalArgumentException("Client certificate is required!")
    }

    private fun RoutingCall.getClientCertificate(): Certificate? {
        val engineCall = this.pipelineCall.engineCall
        val sslHandler = (engineCall as? NettyApplicationCall)?.context?.pipeline()?.get(SslHandler::class.java)
        val chain = sslHandler?.engine()?.session?.peerCertificates
        return chain?.firstOrNull()
    }

    private fun RoutingCall.requireClientCertificate(): Certificate {
        return this.getClientCertificate() ?: throw IllegalArgumentException("Client certificate is required!")
    }


    class PairSession(
        val id: String,
        val clientFingerprint: String,
        val serverNonce: String,
        val expireAt: Long
    )
}
