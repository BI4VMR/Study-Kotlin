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
         * 使用Default调度器，提交任务并启动协程。
         *
         * 此处开启协程时需要调用"join()"方法，否则主线程结束后整个进程将会终止，子线程（协程任务）不会继续执行。
         */
        CoroutineScope(Dispatchers.Default).launch {
            println("Task start. Name:[${getThread()}] Time:[${getTime()}]")
            // 延时2秒，模拟耗时操作。
            delay(2000)
            println("Task end. Name:[${getThread()}] Time:[${getTime()}]")
        }.join()
    }
}

/*
 * 示例：挂起与恢复 - 挂起函数。
 */
fun example02() {
    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 开启任务并获取返回值
            val value = task()
            println("Task return value is $value")
        }.join()
    }
}

// 挂起函数示例
suspend fun task(): Int {
    println("Task start. Name:[${getThread()}] Time:[${getTime()}]")
    delay(2000)
    println("Task end. Name:[${getThread()}] Time:[${getTime()}]")
    return 0
}

/*
 * 示例：挂起与恢复 - 挂起与恢复机制。
 */
fun example03() {
    val scope = CoroutineScope(Dispatchers.Default)
    // 开启第一个任务
    scope.launch {
        task("1", 2000)
    }
    // 开启第二个任务
    scope.launch {
        task("2", 3000)
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

// 挂起函数示例
suspend fun task(name: String, time: Long) {
    println("Task $name start. Name:[${getThread()}] Time:[${getTime()}]")
    delay(time)
    println("Task $name end. Name:[${getThread()}] Time:[${getTime()}]")
}

/*
 * 示例：任务调度 - 顺序执行。
 */
fun example04() {
    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 先执行第一个任务
            task("1", 2000)
            // 第一个任务执行完毕后，再执行第二个任务。
            task("2", 2000)
        }.join()
    }
}

/*
 * 示例：并发执行多个任务。
 */
fun example033() {
    runBlocking {
        println("async start")
        val a = async { function1() }
        delay(5000)
        a.await()
        println("async end")
    }
}

/*
 * 示例：等待多个任务完成再继续执行。
 */
fun example04344() {
    runBlocking {

        val job1: Deferred<Int> = async { function1() }
        val job2: Deferred<Int> = async { function2() }

        val r1 = job1.await()
        val r2 = job2.await()

        println("r=${r1 + r2}")
    }
}

// 测试方法一
suspend fun function1(): Int {
    println("Task1 start. Name:[${getThread()}] Time:[${getTime()}]")
    delay(2000)
    println("Task1 end. Name:[${getThread()}] Time:[${getTime()}]")
    return 7
}

// 测试方法二
suspend fun function2(): Int {
    println("Task2 start. Name:[${getThread()}] Time:[${getTime()}]")
    delay(3000)
    println("Task2 end. Name:[${getThread()}] Time:[${getTime()}]")
    return 3
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

/**
 * 获取当前线程名称。
 *
 * @return 线程名称。
 */
private fun getThread(): String {
    return Thread.currentThread().name
}
