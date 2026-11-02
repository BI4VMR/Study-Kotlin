package net.bi4vmr.androidtoolkit.data.sql.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import net.bi4vmr.androidtoolkit.data.sql.MainDatabase
import net.bi4vmr.androidtoolkit.data.sql.RemoteExecutorQueries
import net.bi4vmr.androidtoolkit.data.sql.model.RemoteExecutorRecord

/**
 * 远程执行器的持久化存储。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RemoteExecutorDAO(
    val database: MainDatabase
) {

    private val queries: RemoteExecutorQueries = database.remoteExecutorQueries

    /**
     * Flow : 所有远程执行器记录，数据变更时自动发出新列表。
     */
    fun observe(): Flow<List<RemoteExecutorRecord>> =
        queries.selectAll(::toModel)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun list(): List<RemoteExecutorRecord> =
        queries.selectAll(::toModel).executeAsList()

    /**
     * 保存记录，地址与端口相同时覆盖旧记录。
     */
    fun save(record: RemoteExecutorRecord) {
        queries.upsert(
            host = record.host,
            port = record.port.toLong(),
            secure = if (record.secure) 1L else 0L,
            added_timestamp = record.addedTimestamp
        )
    }

    fun delete(host: String, port: Int) {
        queries.deleteByAddress(host, port.toLong())
    }

    private fun toModel(
        host: String,
        port: Long,
        secure: Long,
        addedTimestamp: Long
    ): RemoteExecutorRecord = RemoteExecutorRecord(
        host = host,
        port = port.toInt(),
        secure = secure != 0L,
        addedTimestamp = addedTimestamp
    )
}
