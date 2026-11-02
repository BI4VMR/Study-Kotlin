package net.bi4vmr.androidtoolkit.data.sql.model

/**
 * 远程执行器记录。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class RemoteExecutorRecord(

    /**
     * 服务端地址。
     */
    val host: String,

    /**
     * 服务端端口。
     */
    val port: Int,

    /**
     * 是否使用加密连接。
     */
    val secure: Boolean,

    /**
     * 添加时间戳。
     */
    val addedTimestamp: Long
)
