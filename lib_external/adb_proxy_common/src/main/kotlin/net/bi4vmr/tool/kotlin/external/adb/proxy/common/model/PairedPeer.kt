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
     * 证书指纹。
     *
     * 唯一性标识，不同证书指纹应当不同。
     */
    val fingerprint: String,

    /**
     * 显示名称。
     */
    val name: String,

    /**
     * 配对时间戳。
     */
    val pairedTimestamp: Long,

    /**
     * 记录类型。
     */
    val type: PeerType
)
