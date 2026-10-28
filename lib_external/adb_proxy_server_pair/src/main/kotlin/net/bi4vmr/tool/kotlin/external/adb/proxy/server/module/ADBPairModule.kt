package net.bi4vmr.tool.kotlin.external.adb.proxy.server.module

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.netty.handler.ssl.SslHandler
import net.bi4vmr.tool.java.security.digest.MessageDigestUtil
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.API
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairCommitResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.ADBProxyServer
import net.bi4vmr.tool.kotlin.external.adb.proxy.util.PairUtil
import org.slf4j.LoggerFactory
import java.security.cert.Certificate
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger("ADBSecurityRoutes")

// 仅版本查询无需认证；配对接口要求出示客户端证书，其余路径（含 WebSocket 握手）要求客户端已配对。
private val ClientFingerprintKey = AttributeKey<String>("ClientFingerprint")

private val PUBLIC_PATHS = setOf(API.SERVER_VERSION)

private val PAIR_PATHS = setOf(API.PAIR_COMMIT, API.PAIR_REVEAL)

class ADBPairModule : ADBProxyModule {
    // 同一时刻只允许一个配对会话，避免恶意客户端刷屏。
    val session = AtomicReference<PairSession?>(null)

    class PairSession(
        val id: String,
        val clientFingerprint: String,
        val serverNonce: String,
        val expireAt: Long
    )

    override fun Application.register(server: ADBProxyServer) {
        requireNotNull(server.getSSLConfig()) { "No SSL configuration provided. Pairing requires a secure connection." }

        // 注册拦截器，受控接口被访问时，检查客户端证书是否已与服务端配对。
        intercept(ApplicationCallPipeline.Plugins) {
            val path = call.request.path()
            if (path in PUBLIC_PATHS) return@intercept

            val clientCert = requireClientCertificate(call)
            val fingerprint = MessageDigestUtil.getSHA256(clientCert.encoded)
            fingerprint.let { call.attributes.put(ClientFingerprintKey, it) }
            // val allowed = if (path in PAIR_PATHS) fingerprint != null else security.isPaired(fingerprint)
            // if (!allowed) {
            //     call.respondText("Unauthorized, pairing required.", status = HttpStatusCode.Unauthorized)
            //     finish()
            // }
        }


        routing {
            post(API.PAIR_COMMIT) {
                val clientFingerprint = call.attributes.getOrNull(ClientFingerprintKey) ?: return@post
                val request: PairCommitRequest = try {
                    call.receive()
                } catch (e: Exception) {
                    call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                    return@post
                }
                // if (request.clientName.length > 64 || request.commit.length != 64) {
                //     call.respondText("Invalid client information.", status = HttpStatusCode.BadRequest)
                //     return@post
                // }

                val now = System.currentTimeMillis()
                val created = PairSession(
                    id = "1",
                    clientFingerprint = clientFingerprint,
                    serverNonce = PairUtil.generateNonce(),
                    expireAt = now + 5000
                )
                val current = session.get()
                val free = current == null || current.expireAt < now
                if (!free || !session.compareAndSet(current, created)) {
                    call.respondText("Another pairing request is pending.", status = HttpStatusCode.TooManyRequests)
                    return@post
                }
                call.respond(PairCommitResponse(created.id, created.serverNonce))
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

            get(API.PAIR_STATUS) {
                call.respondText("OK", ContentType.Text.Plain)
            }
        }
    }

    private fun requireClientCertificate(call: ApplicationCall): Certificate {
        val sslHandler = (call as NettyApplicationCall).context.pipeline().get(SslHandler::class.java)
        val chain: Array<Certificate> = sslHandler.engine().session.peerCertificates
        return chain.first()
    }
}
