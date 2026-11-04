package net.bi4vmr.androidtoolkit.data.sql.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import net.bi4vmr.androidtoolkit.data.sql.MainDatabase
import net.bi4vmr.androidtoolkit.data.sql.PairedPeerQueries
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums.PeerType
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairedPeer

/**
 * 已配对对端的持久化存储。
 *
 * 基于 SQLDelight 实现，同一个数据库文件只应创建一个实例。
 *
 * @param file 数据库文件。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class PairedPeerDAO(
    val database: MainDatabase
) {

    private val queries: PairedPeerQueries = database.pairedPeerQueries

    /**
     * Flow : 指定类型的已配对对端，数据变更时自动发出新列表。
     */
    fun observe(type: PeerType): Flow<List<PairedPeer>> =
        queries.selectByType(type.name, ::entityToModel)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun list(type: PeerType): List<PairedPeer> =
        queries.selectByType(type.name, ::entityToModel).executeAsList()

    fun find(fingerprint: String): PairedPeer? =
        queries.selectByFingerprint(fingerprint, ::entityToModel).executeAsOneOrNull()

    /**
     * 保存记录，证书指纹相同时覆盖旧记录。
     */
    fun save(peer: PairedPeer) {
        queries.upsert(
            fingerprint = peer.fingerprint,
            name = peer.name,
            paired_timestamp = peer.pairedTimestamp,
            type = peer.type.name
        )
    }

    fun delete(fingerprint: String) {
        queries.deleteByFingerprint(fingerprint)
    }

    private fun entityToModel(
        fingerprint: String,
        name: String,
        pairedTimestamp: Long,
        type: String
    ): PairedPeer = PairedPeer(
        name = name,
        fingerprint = fingerprint,
        pairedTimestamp = pairedTimestamp,
        type = PeerType.valueOf(type)
    )
}
