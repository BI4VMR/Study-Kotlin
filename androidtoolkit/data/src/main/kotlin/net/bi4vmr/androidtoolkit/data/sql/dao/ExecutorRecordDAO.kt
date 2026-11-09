package net.bi4vmr.androidtoolkit.data.sql.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import net.bi4vmr.androidtoolkit.base.model.ExecutorRecord
import net.bi4vmr.androidtoolkit.data.sql.ExecutorRecordQueries
import net.bi4vmr.androidtoolkit.data.sql.MainDatabase
import net.bi4vmr.tool.kotlin.common.base.general.toBoolean
import net.bi4vmr.tool.kotlin.common.base.general.toLong
import net.bi4vmr.tool.kotlin.common.base.time.toTimestamp
import net.bi4vmr.tool.kotlin.common.base.time.toZonedDateTime

/**
 * 数据访问类：远程执行器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ExecutorRecordDAO(
    database: MainDatabase
) {

    private val queries: ExecutorRecordQueries = database.executorRecordQueries


    fun observeAll(): Flow<List<ExecutorRecord>> =
        queries.queryAll(::entityToModel)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun save(record: ExecutorRecord) {
        queries.replace(
            id = record.id,
            name = record.name,
            host = record.host,
            port = record.port.toLong(),
            enable_tls = record.enableTLS.toLong(),
            create_time = record.createTime.toTimestamp(),
            update_time = record.updateTime.toTimestamp()
        )
    }

    fun delete(id: String) {
        queries.deleteByID(id)
    }

    fun checkIfExist(host: String, port: Int): Boolean =
        queries.checkIfExist(host, port.toLong()).executeAsOne()

    private fun entityToModel(
        id: String,
        name: String,
        host: String,
        port: Long,
        enableTLS: Long,
        createTime: Long,
        updateTime: Long
    ): ExecutorRecord = ExecutorRecord(
        id = id,
        name = name,
        host = host,
        port = port.toInt(),
        enableTLS = enableTLS.toBoolean(),
        createTime = createTime.toZonedDateTime(),
        updateTime = updateTime.toZonedDateTime()
    )
}
