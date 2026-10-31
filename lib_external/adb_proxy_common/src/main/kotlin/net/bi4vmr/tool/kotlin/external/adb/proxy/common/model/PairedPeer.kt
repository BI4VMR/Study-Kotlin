package net.bi4vmr.tool.kotlin.external.adb.proxy.common.model

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PeerType

/**
 * 配对模块：已知对端。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
data class PairedPeer(

    /**
     * ID 。
     */
    val id: String,

    /**
     * 名称。
     */
    val name: String,

    /**
     * 证书指纹。
     */
    val fingerprint: String,

    /**
     * 配对时间戳。
     */
    val pairedTimestamp: Long,

    /**
     * 记录类型。
     */
    val type: PeerType
)
