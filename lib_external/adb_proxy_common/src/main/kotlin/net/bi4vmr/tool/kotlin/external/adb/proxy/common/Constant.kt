package net.bi4vmr.tool.kotlin.external.adb.proxy.common

/**
 * 全局常量。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Constant {

    /**
     * 服务端默认端口。
     */
    const val PORT_SERVER_DEFAULT: Int = 5038

    /**
     * 通信协议版本。
     *
     * 出现破坏性变更时应增加版本号，客户端建立连接后应检查双方版本号是否兼容。
     */
    const val PROTOCOL_VERSION: String = "1"
}
