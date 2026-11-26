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
     * 文件传输缓冲区大小（字节）。
     *
     * 分片传输文件时，每个分片最多为该参数所指定的长度；这些数据将在内存中暂存，因此不宜设置过大，避免导致发送端内存溢出。
     */
    const val FILE_SEGMENT_SIZE: Int = 16 * 1024 * 1024

    /**
     * 配对会话超时时间（毫秒）。
     *
     * 从客户端提交承诺（[APIs.PAIR_COMMIT]）开始计时。超时后会话失效，等待中的确认请求返回超时结果。
     */
    const val PAIR_TIMEOUT_MS: Long = 60_000L
}
