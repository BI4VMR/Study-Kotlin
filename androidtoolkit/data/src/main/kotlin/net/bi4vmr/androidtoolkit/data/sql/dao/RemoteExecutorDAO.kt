package net.bi4vmr.androidtoolkit.data.sql.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import net.bi4vmr.androidtoolkit.data.sql.MainDatabase
import net.bi4vmr.androidtoolkit.data.sql.RemoteExecutorQueries
import net.bi4vmr.androidtoolkit.data.sql.model.RemoteExecutorRecord
import net.bi4vmr.tool.kotlin.common.base.toBoolean
import net.bi4vmr.tool.kotlin.common.base.toLong

/**
 * 数据访问类：远程执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RemoteExecutorDAO(
    database: MainDatabase
) {

    private val queries: RemoteExecutorQueries = database.remoteExecutorQueries


    /**
     * Flow : 所有远程执行器记录，数据变更时自动发出新列表。
     */
    fun observe(): Flow<List<RemoteExecutorRecord>> =
        queries.selectAll(::entityToModel)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun queryAll(): List<RemoteExecutorRecord> =
        queries.selectAll(::entityToModel).executeAsList()

    /**
     * 保存记录，地址与端口相同时覆盖旧记录。
     */
    fun save(record: RemoteExecutorRecord) {
        queries.replace(
            host = record.host,
            port = record.port.toLong(),
            enable_tls = record.enableTLS.toLong(),
            create_time = record.createTime
        )
    }

    fun delete(host: String, port: Int) {
        queries.deleteByAddress(host, port.toLong())
    }

    private fun entityToModel(
        host: String,
        port: Long,
        enableTLS: Long,
        addedTimestamp: Long
    ): RemoteExecutorRecord = RemoteExecutorRecord(
        host = host,
        port = port.toInt(),
        enableTLS = enableTLS.toBoolean(),
        createTime = addedTimestamp
    )
}
