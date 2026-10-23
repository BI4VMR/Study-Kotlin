package net.bi4vmr.androidtoolkit.ui

import androidx.compose.runtime.mutableStateListOf

/**
 * 全局窗口管理器。
 */
object GlobalWindowManager {

    /**
     * 递增序号。
     *
     * 用于唯一标识每个窗口。
     */
    private var currentID: Long = 1L

    /**
     * 窗口列表。
     */
    val windows = mutableStateListOf<Record>()


    /**
     * 添加窗口。
     */
    fun addWindow(record: Record) {
        windows.add(record)
    }

    /**
     * 移除窗口。
     */
    fun removeWindow(record: Record) {
        windows.remove(record)
    }


    /**
     * 窗口记录。
     *
     * 调用者可以继承该类并添加一些自定义属性，以便在显示窗口时传递数据。
     */
    open class Record {
        val id: Long = currentID++
    }
}
