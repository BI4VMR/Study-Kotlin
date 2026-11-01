package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult

/**
 * 配对模块：配对请求。
 *
 * 与 [PairSession] 相比，只保留需要向用户展示的信息，适合界面层进行显示。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PairRequest(

    /**
     * 配对会话。
     */
    private val session: PairSession
) {

    /**
     * 获取客户端名称。
     */
    fun getClientName(): String = session.clientName

    /**
     * 获取客户端证书指纹。
     */
    fun getClientFingerprint(): String = session.clientFingerprint

    /**
     * 获取鉴权短码。
     */
    fun getSASCode(): String =
        // 该对象仅在第二阶段配对码生成后才通过回调公开给上层，此时配对码不应当为空。
        requireNotNull(session.sasCode)

    /**
     * 批准配对请求。
     */
    fun approve() {
        session.setResult(PairResult.APPROVED)
    }

    /**
     * 拒绝配对请求。
     */
    fun reject() {
        session.setResult(PairResult.REJECTED)
    }
}
