package net.bi4vmr.study.scope

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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


/*
 * 示例：编码风格。
 */

// 不推荐的用法
class UserManager {

    fun updateCurrentUID() {
        CoroutineScope(Dispatchers.IO).launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }

    fun updateCurrentName() {
        CoroutineScope(Dispatchers.IO).launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }
}

// 推荐用法一：由调用者提供协程环境。
class UserManagerV1 {

    suspend fun updateCurrentUID(): String = "1"

    suspend fun updateCurrentName(): String = "User1"
}

// 推荐用法二：调用者无法使用协程，由当前类维护协程环境。
class UserManagerV2(
    // 使用外部传入的调度器，或提供默认的调度器。
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // 所有业务使用统一的作用域
    private val scope = CoroutineScope(dispatcher)

    fun updateCurrentUID() {
        // 方法内部不要单独创建作用域
        scope.launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }

    fun updateCurrentName() {
        // 方法内部不要单独创建作用域
        scope.launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }

    // 提供组件销毁方法，取消当前作用域的所有协程。
    fun destroy() {
        scope.cancel()
    }
}
