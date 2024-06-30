package net.bi4vmr.study.coroutine

import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 测试代码 - 协程。
 *
 * @author BI4VMR
 */
fun main() {
    example06()
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
    // 挂起函数示例
    suspend fun task(): Int {
        println("Task start. Name:[${getThread()}] Time:[${getTime()}]")
        delay(2000)
        println("Task end. Name:[${getThread()}] Time:[${getTime()}]")
        return 0
    }

    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 开启任务并获取返回值
            val value = task()
            println("Task return value is $value")
        }.join()
    }
}

/*
 * 示例：挂起与恢复 - 挂起与恢复机制。
 */
fun example03() {
    // 挂起函数示例
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Name:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Name:[${getThread()}] Time:[${getTime()}]")
    }

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

/*
 * 示例：取消任务（无效示范）。
 */
fun example04() {
    // 启动一个协程，循环输出日志信息。
    val job: Job = CoroutineScope(Dispatchers.Default).launch {
        for (i in 1..10_000) {
            println("Task start. Index:[$i]")
        }
    }

    // 主线程等待25毫秒，然后取消协程任务。
    Thread.sleep(25L)
    job.cancel()
    println("Try to cancel task.")

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/*
 * 示例：取消任务（非挂起状态）。
 */
fun example05() {
    // 启动一个协程，循环输出日志信息。
    val job: Job = CoroutineScope(Dispatchers.Default).launch {
        for (i in 1..10_000) {
            // 每轮循环开始前，先判断当前任务是否已被取消。
            if (!isActive) {
                // 任务被取消时，终止后续任务。
                println("Task was canceled!")
                return@launch
            }

            println("Task start. Index:[$i]")
        }
    }

    // 主线程等待25毫秒，然后取消协程任务。
    Thread.sleep(25L)
    println("Try to cancel task.")
    job.cancel()

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/*
 * 示例：取消任务（挂起状态）。
 */
fun example06() {
    // 启动一个协程，循环输出日志信息。
    val job: Job = CoroutineScope(Dispatchers.Default).launch {
        try {
            println("Task start.")
            delay(2000L)
            println("Task end.")
        } catch (e: CancellationException) {
            println("Catch cancellation exception!")
        }
    }

    // 主线程等待25毫秒，然后取消协程任务。
    Thread.sleep(25L)
    println("Try to cancel task.")
    job.cancel()

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
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
