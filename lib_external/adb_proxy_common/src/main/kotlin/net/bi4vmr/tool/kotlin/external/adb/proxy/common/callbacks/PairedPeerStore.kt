package net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairedPeer

/**
 * 配对模块：已知对等体信息存储。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
interface PairedPeerStore {

    /**
     * 列出所有已知对等体。
     *
     * @return 对等体列表。
     */
    fun onList(): List<PairedPeer>

    /**
     * 保存对等体记录。
     *
     * @param[peer] 对等体记录。
     */
    fun onSave(peer: PairedPeer)

    /**
     * 删除对等体记录。
     *
     * @param[fingerprint] 证书指纹。
     */
    fun onDelete(fingerprint: String)
}
