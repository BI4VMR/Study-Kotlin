package net.bi4vmr.study.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 示例代码：数据同步。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例一：竞态条件。
 */
fun example01() {
    // 累加变量
    var i = 0

    // 读取当前变量值，自增后写回全局变量。
    fun add() {
        i++
    }

    // 创建10000个协程，每个协程执行一次自增操作。
    repeat(10000) {
        CoroutineScope(Dispatchers.Default)
            .launch { add() }
    }

    // 测试线程休眠片刻，等待所有协程执行完毕。
    Thread.sleep(250L)
    // 检查最终累加变量的值
    println("最终结果：[$i]")
}


/**
 * 示例二：互斥锁的基本用法。
 */
fun example02() {
    // 累加变量
    var i = 0

    // 创建互斥锁对象
    val mutex = Mutex()

    // 读取当前变量值，自增后写回全局变量。
    suspend fun add() {
        // 进入临界区，锁定互斥锁，阻止其他协程并发修改。
        mutex.lock()
        try {
            // 临界区代码
            i++
        } finally {
            // 退出临界区，释放互斥锁，允许其他协程继续操作。
            mutex.unlock()
        }
    }

    // 创建10000个协程，每个协程执行一次自增操作。
    repeat(10000) {
        CoroutineScope(Dispatchers.Default)
            .launch { add() }
    }

    // 测试线程休眠片刻，等待所有协程执行完毕。
    Thread.sleep(250L)
    // 检查最终累加变量的值
    println("最终结果：[$i]")
}

fun example03() {
    // 累加变量
    var i = 0

    // 创建互斥锁对象
    val mutex = Mutex()

    // 读取当前变量值，自增后写回全局变量。
    suspend fun add() {
        // 第二参数 `action` 即临界区，开始执行前自动锁定，执行完毕后自动解锁。
        mutex.withLock {
            // 执行非原子操作
            i++
        }
    }

    // 创建10000个协程，每个协程执行一次自增操作。
    repeat(10000) {
        CoroutineScope(Dispatchers.Default)
            .launch { add() }
    }

    // 测试线程休眠片刻，等待所有协程执行完毕。
    Thread.sleep(250L)
    // 检查最终累加变量的值
    println("最终结果：[$i]")
}
