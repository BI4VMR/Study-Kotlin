package net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PairResult
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairRequest

/**
 * 配对请求监听器（服务端）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface PairRequestListener {

    /**
     * 客户端发起配对请求。
     *
     * 此时服务端应当显示配对码以供用户核对。
     */
    fun onRequest(request: PairRequest)

    /**
     * 配对流程结束。
     *
     * 服务端批准、拒绝或超时结束。
     */
    fun onResult(request: PairRequest, result: PairResult)
}
