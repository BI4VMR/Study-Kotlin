package net.bi4vmr.tool.kotlin.external.adb.cast.proxy.common

import net.bi4vmr.tool.kotlin.external.adb.cast.proxy.model.dto.ADBCastStartRequest

/**
 * URL 定义。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object CastAPI {

    /**
     * 请求远程设备开启投屏转发。
     *
     * 请求方式为 HTTP POST ，请求体为 [ADBCastStartRequest] ，返回纯文本，内容为服务端转发端口，客户端需要使用 `ADBCastReceiver` 建立连接。
     */
    const val ADB_CAST_START: String = "/adb/cast/start"
}
