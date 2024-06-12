package net.bi4vmr.study.base

import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 测试代码 - 基本应用。
 *
 * @author BI4VMR
 */
fun main() {
    example02()
}

/*
 * 示例：基本应用。
 *
 * 若在Android等主线程无限循环的环境中实验，无需调用"runBlocking{}"和"join()"方法。
 */
fun example01() {
    // 阻塞主线程，等待协程执行完毕。
    runBlocking {
        /*
         * 使用Default调度器，启动协程并提交任务。
         *
         * 此处开启协程时需要调用"join()"方法，否则主线程结束后整个进程将会终止，子线程（协程任务）不会继续执行。
         */
        CoroutineScope(Dispatchers.Default).launch {
            println("Coroutine exec start. Time:[${getTime()}]")
            // 延时2秒，模拟耗时操作。
            delay(2000)
            println("Coroutine exec end. Time:[${getTime()}]")
        }.join()
    }
}

/*
 * 示例：顺序执行。
 */
fun example02() {
    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 先执行第一个任务
            function1()
            // 第一个任务执行完毕后，再执行第二个任务。
            function2()
        }.join()
    }
}

suspend fun function1() {
    println("Suspend function1 exec start. Time:[${getTime()}]")
    delay(2000)
    println("Suspend function1 exec end. Time:[${getTime()}]")
}

suspend fun function2() {
    println("Suspend function2 exec start. Time:[${getTime()}]")
    delay(3000)
    println("Suspend function2 exec end. Time:[${getTime()}]")
}

/**
 * 获取当前时间。
 *
 * @return 时间字符串（HH:mm:ss.SSS）。
 */
private fun getTime(): String {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    return LocalTime.now().format(dateFormatter)
}
