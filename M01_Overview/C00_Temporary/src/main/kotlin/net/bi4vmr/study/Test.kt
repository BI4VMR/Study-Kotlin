package net.bi4vmr.study

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    val jobs = List(10) { // 启动10个协程
        launch(Dispatchers.Default) {
            repeat(10) { // 每个协程递增计数器1000次
                mutex.withLock { // 自动加锁/解锁
                    println("th: ${Thread.currentThread().name} mutex isLocked:${mutex.isLocked}")
                    delay(10L)
                    counter++
                }
            }
        }
    }
    jobs.forEach { it.join() }
    println("Final counter: $counter") // 正确输出10000
}

/**
 * 示例：retrun
 */
fun example01() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    array.forEach {
        if (it == 2) {
            // 返回foreach之外
            return
        }

        println("Loop $it")
    }

    println("Some statement after loop...")
}

fun example02() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    array.forEach {
        if (it == 2) {
            // 相当于"for"循环中的"continue"
            return@forEach
        }

        println("Loop $it")
    }

    println("Some statement after loop...")
}

/**
 * 示例：类似break
 */
fun example03() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    run loop@ {
        array.forEach {
            if (it == 3) {
                // 相当于"for"循环中的"break"
                return@loop
            }

            println("Loop $it")
        }
    }

    println("Some statement after loop...")
}
