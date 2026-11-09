package net.bi4vmr.androidtoolkit.data.sql

import app.cash.sqldelight.TransactionWithReturn
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import net.bi4vmr.androidtoolkit.data.sql.dao.PairedPeerDAO
import net.bi4vmr.androidtoolkit.data.sql.dao.RemoteDeviceDAO
import net.bi4vmr.androidtoolkit.data.sql.dao.ExecutorRecordDAO
import java.io.File

/**
 * 主要数据库。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object MainDB {

    private const val FILE_NAME: String = "main.db"

    @Volatile
    private lateinit var instance: MainDatabase


    val remoteDeviceDAO: RemoteDeviceDAO by lazy {
        checkInit()
        RemoteDeviceDAO(instance)
    }

    val executorRecordDAO: ExecutorRecordDAO by lazy {
        checkInit()
        ExecutorRecordDAO(instance)
    }

    val pairedPeerDAO: PairedPeerDAO by lazy {
        checkInit()
        PairedPeerDAO(instance)
    }


    fun init(dataPath: String) {
        val dbFile = File(dataPath, FILE_NAME)
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        val driver = JdbcSqliteDriver(url = url, schema = MainDatabase.Schema)
        instance = MainDatabase.Companion(driver)
    }

    fun <R> withTransaction(block: TransactionWithReturn<R>.() -> R) = instance.transactionWithResult { block() }


    private fun checkInit() {
        if (!::instance.isInitialized) {
            throw IllegalStateException("DB is not initialized, Had you call `init()` at first?")
        }
    }
}
