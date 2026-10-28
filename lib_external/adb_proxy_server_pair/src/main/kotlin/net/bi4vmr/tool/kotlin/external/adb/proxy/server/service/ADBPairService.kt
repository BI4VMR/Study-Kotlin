package net.bi4vmr.tool.kotlin.external.adb.proxy.server.service

import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairCommitRequest
import net.bi4vmr.tool.kotlin.external.adb.proxy.model.dto.PairCommitResponse
import net.bi4vmr.tool.kotlin.external.adb.proxy.util.PairUtil

/**
 * ADB 代理配对服务。
 *
 * 管理客户端与服务端的配对关系。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBPairService {

    private val pairSessions: List<PairSession> = mutableListOf()

    // 开始配对，交换随机数Hash
    fun receiveCommit(request: PairCommitRequest): PairCommitResponse {
        val sessionID = PairUtil.generateNonce()
        val nonce = PairUtil.generateNonce()
        val commit = PairUtil.calculateCommit("f", nonce)

        val session = PairSession(
            id = sessionID,
            serverNonce = nonce,
            serverCommit = commit,
            clientCommit = request.clientHash
        )

        // 3. 返回响应，包含随机数的Hash值
        return PairCommitResponse(commit)
    }

    // 第二阶段，交换公钥指纹与随机数，检查是否相符。
    fun receiveReveal() {

    }

    data class PairSession(
        val id: String,
        val serverNonce: String,
        val serverCommit: String,
        val clientNonce: String = "",
        val clientCommit: String = "",
        val clientFingerprint: String = "",
    )
}
