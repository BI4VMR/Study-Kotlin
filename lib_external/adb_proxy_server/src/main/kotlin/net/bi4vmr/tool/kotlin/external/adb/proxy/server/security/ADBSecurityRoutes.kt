package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.withTimeoutOrNull
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.API
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.PairingCode
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairInitResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairResponse
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger("ADBSecurityRoutes")

// 版本查询与配对不需要令牌，其余路径（含 WebSocket 握手）都必须通过令牌校验。
private val PUBLIC_PATHS = setOf(API.VERSION, API.PAIR_INIT, API.PAIR_REVEAL)

private const val BEARER = "Bearer "

/**
 * 安装认证拦截器与配对路由。
 *
 * 需在 `ContentNegotiation` 之后调用。
 */
internal fun Application.installSecurity(security: ADBProxySecurity) {
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.path() in PUBLIC_PATHS) return@intercept

        val header = call.request.headers[HttpHeaders.Authorization]
        val token = header?.takeIf { it.startsWith(BEARER) }?.substring(BEARER.length)?.trim()
        if (!security.isTokenValid(token)) {
            call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer")
            call.respondText("Unauthorized, pairing required.", status = HttpStatusCode.Unauthorized)
            finish()
        }
    }

    // 同一时刻只允许一个配对会话，避免恶意客户端刷屏。
    val session = AtomicReference<PairSession?>(null)

    routing {
        post(API.PAIR_INIT) {
            val request: PairRequest = try {
                call.receive()
            } catch (e: Exception) {
                call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                return@post
            }
            if (request.clientId.isBlank() || request.clientId.length > 64 ||
                request.clientName.length > 64 || request.commit.length != 64
            ) {
                call.respondText("Invalid client information.", status = HttpStatusCode.BadRequest)
                return@post
            }

            val now = System.currentTimeMillis()
            val created = PairSession(
                id = ADBProxySecurity.generateToken(),
                request = request,
                serverNonce = PairingCode.newNonce(),
                expireAt = now + ADBProxySecurity.PAIR_TIMEOUT_MS
            )
            val current = session.get()
            val free = current == null || current.expireAt < now
            if (!free || !session.compareAndSet(current, created)) {
                call.respondText("Another pairing request is pending.", status = HttpStatusCode.TooManyRequests)
                return@post
            }
            call.respond(PairInitResponse(created.id, created.serverNonce))
        }

        post(API.PAIR_REVEAL) {
            val reveal: PairRevealRequest = try {
                call.receive()
            } catch (e: Exception) {
                call.respondText("Invalid request body.", status = HttpStatusCode.BadRequest)
                return@post
            }

            // 会话只能使用一次，无论成败都会被消耗，防止对承诺或配对码的重复尝试。
            val current = session.get()
            if (current == null || current.id != reveal.pairId || !session.compareAndSet(current, null)) {
                call.respondText("Pairing session not found.", status = HttpStatusCode.NotFound)
                return@post
            }
            val remaining = current.expireAt - System.currentTimeMillis()
            if (remaining <= 0) {
                call.respondText("Pairing session expired.", status = HttpStatusCode.Gone)
                return@post
            }
            if (PairingCode.commit(reveal.clientNonce) != current.request.commit) {
                call.respondText("Commitment mismatch.", status = HttpStatusCode.BadRequest)
                return@post
            }

            val code = PairingCode.compute(security.fingerprint, reveal.clientNonce, current.serverNonce)
            val approved = security.trustAllClients ||
                    withTimeoutOrNull(remaining) { security.pairingApprover(current.request, code) } == true
            if (!approved) {
                logger.info("Pairing request from [{}] was rejected or timed out.", current.request.clientName)
                call.respondText("Pairing rejected.", status = HttpStatusCode.Forbidden)
                return@post
            }

            val token = ADBProxySecurity.generateToken()
            security.store.save(
                PairedClient(
                    id = current.request.clientId,
                    name = current.request.clientName,
                    tokenHash = ADBProxySecurity.hashToken(token),
                    pairedAt = System.currentTimeMillis()
                )
            )
            logger.info("Client [{}] has been paired.", current.request.clientName)
            call.respond(PairResponse(token))
        }

        get(API.PAIR_STATUS) {
            call.respondText("OK", ContentType.Text.Plain)
        }
    }
}

private class PairSession(
    val id: String,
    val request: PairRequest,
    val serverNonce: String,
    val expireAt: Long
)
