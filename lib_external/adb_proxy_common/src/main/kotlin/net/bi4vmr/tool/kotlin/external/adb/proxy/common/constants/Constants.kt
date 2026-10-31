package net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants

/**
 * 全局常量。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Constants {

    /**
     * 服务端默认端口。
     */
    const val PORT_SERVER_DEFAULT: Int = 5038

    /**
     * 通信协议版本。
     *
     * 出现不兼容的变更时应提升版本号，客户端建立连接后应检查双方版本号是否兼容。
     */
    const val PROTOCOL_VERSION: String = "1"

    /**
     * 配对会话超时时间（毫秒）。
     *
     * 从客户端提交承诺（[APIs.PAIR_COMMIT]）开始计时。超时后会话失效，等待中的确认请求返回超时结果。
     */
    const val PAIR_TIMEOUT_MS: Long = 60_000L
}
