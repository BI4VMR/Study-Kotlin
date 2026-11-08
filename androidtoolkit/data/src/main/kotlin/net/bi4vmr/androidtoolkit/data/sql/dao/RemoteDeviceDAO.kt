package net.bi4vmr.androidtoolkit.data.sql.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import net.bi4vmr.androidtoolkit.data.sql.MainDatabase
import net.bi4vmr.androidtoolkit.data.sql.RemoteDeviceQueries
import net.bi4vmr.androidtoolkit.data.sql.model.RemoteDeviceRecord
import net.bi4vmr.tool.kotlin.common.base.time.toTimestamp
import net.bi4vmr.tool.kotlin.common.base.time.toZonedDateTime

/**
 * 数据访问类：远程设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RemoteDeviceDAO(
    database: MainDatabase
) {

    private val queries: RemoteDeviceQueries = database.remoteDeviceQueries


    fun observeByExecutorID(executorID: String): Flow<List<RemoteDeviceRecord>> =
        queries.queryByExecutorID(executorID, ::entityToModel)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun replace(record: RemoteDeviceRecord) {
        queries.replace(
            id = record.id,
            host = record.host,
            port = record.port.toLong(),
            create_time = record.createTime.toTimestamp(),
            update_time = record.updateTime.toTimestamp(),
            executor_id = record.executorID
        )
    }

    // fun deleteByID(host: String, port: Int) {
    //     queries.deleteByID(host, port.toLong())
    // }

    fun checkIfExist(host: String, port: Int, executorID: String): Boolean =
        queries.checkIfExist(host, port.toLong(), executorID).executeAsOne()


    private fun entityToModel(
        id: String,
        host: String,
        port: Long,
        createTime: Long,
        updateTime: Long,
        executorID: String
    ): RemoteDeviceRecord = RemoteDeviceRecord(
        id = id,
        host = host,
        port = port.toInt(),
        createTime = createTime.toZonedDateTime(),
        updateTime = updateTime.toZonedDateTime(),
        executorID = executorID
    )
}
