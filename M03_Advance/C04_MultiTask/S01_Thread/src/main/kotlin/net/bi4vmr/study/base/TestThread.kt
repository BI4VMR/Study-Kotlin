package net.bi4vmr.study.base

import kotlin.concurrent.thread

/**
 * 示例代码：线程。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例一：创建协程作用域。
 *
 * 在本示例中，我们创建一些协程作用域，并指定上下文。
 */
fun example01() {
    // 创建线程并立即启动
    thread {
        val tName = Thread.currentThread().name
        println("Thread $tName executing...")
    }


    // 创建线程并稍后按需启动
    val thread: Thread = thread(start = false, name = "NamedThread") {
        val tName = Thread.currentThread().name
        println("Thread $tName executing...")
    }

    // 通过 `start()` 方法启动线程
    println("Start named thread.")
    thread.start()
}
