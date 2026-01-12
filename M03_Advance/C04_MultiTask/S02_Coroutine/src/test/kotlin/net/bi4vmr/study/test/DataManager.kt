package net.bi4vmr.study.test

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 文件管理（模拟）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DataManager(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher)

    var freeSpace: Int = 0
        private set

    fun clearCache() {
        scope.launch {
            // 模拟耗时操作...
            delay(1000L)

            // 清理完毕，增加可用空间。
            freeSpace = 100
            println("Clean completed in coroutine task.")
        }
    }
}
