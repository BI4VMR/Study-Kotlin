package net.bi4vmr.androidtoolkit.data.sql.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import net.bi4vmr.androidtoolkit.base.model.DeviceRecord
import net.bi4vmr.androidtoolkit.data.sql.DeviceRecordQueries
import net.bi4vmr.androidtoolkit.data.sql.MainDatabase
import net.bi4vmr.tool.kotlin.common.base.time.toTimestamp
import net.bi4vmr.tool.kotlin.common.base.time.toZonedDateTime

/**
 * 数据访问类：远程设备记录。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DeviceRecordDAO(
    database: MainDatabase
) {

    private val queries: DeviceRecordQueries = database.deviceRecordQueries


    fun observeAll(): Flow<List<DeviceRecord>> =
        queries.queryAll(::entityToModel)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun queryByAddress(host: String, port: Int): DeviceRecord? =
        queries.queryByAddress(host, port.toLong(), ::entityToModel).executeAsOneOrNull()

    fun replace(record: DeviceRecord) {
        queries.replace(
            id = record.id,
            host = record.host,
            port = record.port.toLong(),
            create_time = record.createTime.toTimestamp(),
            update_time = record.updateTime.toTimestamp()
        )
    }

    fun deleteByID(id: String) {
        queries.deleteByID(id)
    }

    fun deleteByAddress(host: String, port: Int) {
        queries.deleteByAddress(host, port.toLong())
    }

    fun checkIfExist(host: String, port: Int): Boolean =
        queries.checkIfExist(host, port.toLong()).executeAsOne()


    private fun entityToModel(
        id: String,
        host: String,
        port: Long,
        createTime: Long,
        updateTime: Long
    ): DeviceRecord = DeviceRecord(
        id = id,
        host = host,
        port = port.toInt(),
        createTime = createTime.toZonedDateTime(),
        updateTime = updateTime.toZonedDateTime()
    )
}
