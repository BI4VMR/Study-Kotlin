package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import kotlinx.coroutines.*
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks.PairedPeerStore
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PeerType
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairSession
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairedPeer
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairCommitResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.dto.PairRevealResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.security.SSLConfig
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.util.PairUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * ADB 代理配对服务。
 *
 * 管理客户端与服务端的配对关系，以及配对过程中等待用户确认的会话。
 *
 * 配对分为三个阶段：
 * 1. 客户端提交承诺值（[onCommit]）；
 * 2. 客户端公开随机数，服务端计算配对码并保存（[onReveal]）；
 * 3. 客户端发起确认请求时通知界面并等待用户批准（[onConfirm]），期间任意一端均可取消（[onCancel]）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBPairService(

    /**
     * SSL 配置。
     */
    val sslConfig: SSLConfig,

    /**
     * 已知对等体信息存储。
     */
    val pairedPeerStore: PairedPeerStore?,

    /**
     * 配对请求回调方法。
     */
    val onPairRequest: ((PairRequest) -> Unit)?
) {

    /**
     * 日志工具。
     */
    private val logger = LoggerFactory.getLogger(ADBPairService::class.java)

    /**
     * 任务作用域。
     */
    private val taskScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 配对会话集合。
     *
     * 键名为客户端证书指纹，键值为会话数据。
     */
    private val pairSessions: MutableMap<String, PairSession> = ConcurrentHashMap()


    /* ----- 服务管理 ----- */

    /**
     * 释放资源。
     */
    fun release() {
        pairSessions.values.forEach { it.abort() }
        pairSessions.clear()

        taskScope.cancel()
    }


    /* ----- 配对流程 ----- */

    /**
     * 第一阶段。
     *
     * 双方交换会话随机数与证书指纹的消息摘要。
     */
    fun onCommit(cFingerprint: String, body: PairCommitRequest): PairCommitResponse {
        /* 清理旧的会话 */
        pairSessions.remove(cFingerprint)?.abort()

        /* 开始新的会话 */
        val sNonce = PairUtil.generateNonce()
        val sFingerprint = PairUtil.calculateCertFingerprint(sslConfig.requireCertificate())
        val sCommit = PairUtil.calculateCommit(sFingerprint, sNonce)

        val session = PairSession(
            clientFingerprint = cFingerprint,
            clientHash = body.clientHash,
            serverNonce = sNonce
        )
        session.expireJob = taskScope.launch {
            delay(Constants.PAIR_TIMEOUT_MS)

            // 超时时间到达时，移除记录并将其标记为过期。
            if (pairSessions.remove(cFingerprint, session)) {
                logger.info("Pair session for client [{}] has expired.", cFingerprint)
                session.setResult(PairResult.EXPIRED)
            }
        }
        logger.info("Pair session for client [{}] created.", cFingerprint)
        pairSessions[cFingerprint] = session

        return PairCommitResponse(sCommit)
    }

    /**
     * 第二阶段。
     *
     * 交换公钥指纹与随机数，检查它们的消息摘要是否与第一阶段相符。
     */
    fun onReveal(cFingerprint: String, body: PairRevealRequest): PairRevealResponse {
        val session = pairSessions[cFingerprint]
            ?: throw IllegalStateException("No pairing session found for client [$cFingerprint].")

        /* 检查随机数是否与前一步的消息摘要匹配 */
        val expected = PairUtil.calculateCommit(cFingerprint, body.clientNonce)
        if (expected != session.clientHash) {
            pairSessions.remove(cFingerprint, session)
            session.expireJob?.cancel()
            throw IllegalStateException("Client hash does not match for client [$cFingerprint].")
        }

        /* 计算配对码 */
        val sFingerprint = PairUtil.calculateCertFingerprint(sslConfig.requireCertificate())
        val code = PairUtil.calculatePairCode(cFingerprint, sFingerprint, body.clientNonce, session.serverNonce)
        session.sasCode = code

        session.clientID = body.id
        session.clientName = body.name

        return PairRevealResponse(session.serverNonce)
    }

    /**
     * 第三阶段：通知界面并等待用户批准或拒绝。
     *
     * 客户端发出确认请求后才通知界面展示配对码，确保服务端用户的批准结果一定有人接收；
     * 若客户端尚未请求确认，服务端不会弹出提示。
     *
     * 本方法会挂起，直至用户操作、客户端取消（[onCancel]）、会话超时或请求被取消。
     *
     * @return 配对结果，界面或客户端据此展示最终状态。
     * @throws[IllegalStateException] 会话不存在、已过期或尚未进入第二阶段。
     */
    suspend fun onConfirm(cFingerprint: String): PairResult {
        val session = pairSessions[cFingerprint]
            ?: throw IllegalStateException("No pairing session found for client [$cFingerprint].")

        // 通知上层显示配对码
        runCatching { onPairRequest?.invoke(PairRequest(session)) }
            .onFailure { logger.warn("Failed to notify pairing approval listener.", it) }

        // 等待用户确认
        val result = try {
            session.await()
        } catch (e: CancellationException) {
            // 客户端断开连接或服务端停止：结束会话并关闭界面中的申请，避免残留在界面上。
            // onCancel(cFingerprint)
            // throw e
            println("session.await() on cancel")
            PairResult.ABORTED
        }

        session.expireJob?.cancel()
        pairSessions.remove(cFingerprint, session)
        logger.info("Pairing result for client [{}]: [{}].", cFingerprint, result)

        /* 配对通过后存储凭据 */
        if (result == PairResult.APPROVED) {
            val peer = PairedPeer(
                id = session.clientID,
                name = session.clientName,
                fingerprint = cFingerprint,
                pairedTimestamp = System.currentTimeMillis(),
                type = PeerType.CLIENT
            )
            pairedPeerStore?.onSave(peer)
        }

        return result
    }

    /**
     * 客户端请求取消配对。
     */
    fun onCancel(cFingerprint: String) {
        logger.info("Pairing session for client [{}] has been cancelled.", cFingerprint)

        // 移除该客户端的配对记录，若记录存在同时将其标记为取消。
        pairSessions.remove(cFingerprint)?.cancel()
    }
}
