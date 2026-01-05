package net.bi4vmr.study.scope

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 测试代码：协程环境。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例：基本应用。
 *
 * 在本示例中，我们创建一个协程。
 */
fun example01() {
}


class UserManager {

    private var currentUID: String = "0"

    fun updateCurrentUID() {
        CoroutineScope(Dispatchers.IO).launch {
            // 模拟耗时操作完成后更新结果
            currentUID = "User1"
        }
    }
}


class UserManager2 {

    // 模拟耗时操作完成后返回结果
    suspend fun updateCurrentUID(): String = "User1"
}


class UserManager3(
    // 使用外部传入的调度器，或提供默认的调度器。
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Manager内的业务使用统一作用域
    private val scope = CoroutineScope(dispatcher)

    private var currentUID: String = "0"

    fun updateCurrentUID() {
        // 方法内部不要单独创建作用域
        scope.launch {
            // 模拟耗时操作完成后更新结果
            currentUID = "User1"
        }
    }

    // 提供组件销毁方法，取消作用域的所有协程。
    fun destroy() {
        scope.cancel()
    }
}
