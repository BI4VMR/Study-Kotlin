package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult

/**
 * 配对模块：配对会话。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PairSession(

    /**
     * 客户端证书指纹。
     */
    val clientFingerprint: String,

    /**
     * 客户端的承诺值。
     */
    val clientHash: String,

    /**
     * 服务端随机数。
     */
    val serverNonce: String
) {
    /**
     * 创建时间点（纳秒）。
     */
    val createdTime: Long = System.nanoTime()

    /**
     * 客户端名称。
     */
    var clientName: String = ""

    /**
     * 配对码。
     */
    var sasCode: String? = null

    /**
     * 配对结果 Deferred 。
     *
     * 任务协程可挂起并等待用户选择配对结果。
     */
    private val deferred: CompletableDeferred<PairResult> = CompletableDeferred()

    /**
     * 会话超时任务。
     */
    @Volatile
    var expireJob: Job? = null


    /**
     * 挂起调用协程并等待配对结果。
     *
     * @return 配对结果。
     */
    suspend fun await(): PairResult = deferred.await()

    /**
     * 设置结果。
     *
     * 唤醒任务线程并通知配对结果。
     *
     * @param[result] 配对结果。
     */
    fun setResult(result: PairResult) {
        expireJob?.cancel()
        expireJob = null

        deferred.complete(result)
    }

    /**
     * 客户端取消配对。
     *
     * [setResult] 的快捷方法，将任务结果标记为 [PairResult.CANCELLED] 。
     */
    fun cancel() = setResult(PairResult.CANCELLED)

    /**
     * 终止配对。
     *
     * [setResult] 的快捷方法，将任务结果标记为 [PairResult.ABORTED] 。
     */
    fun abort() = setResult(PairResult.ABORTED)
}
