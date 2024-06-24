package net.bi4vmr.study.coroutine

import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 测试代码 - 协程。
 *
 * @author BI4VMR
 */
fun main() {
    example01()
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
 * 示例：任务调度 - 顺序执行任务。
 */
fun example04() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Name:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Name:[${getThread()}] Time:[${getTime()}]")
    }

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
 * 示例：任务调度 - 并发执行任务。
 */
fun example05() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Name:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Name:[${getThread()}] Time:[${getTime()}]")
    }

    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 使用"launch()"方法开启任务，不接收返回值。
            launch { task("1", 2000) }

            // 使用"async()"方法开启任务，并通过变量保存任务实例，以便后续获取返回值。
            val job: Deferred<Int> = async {
                task("2", 2000)
                114514
            }
            // 异步等待任务结束，并接收返回值。
            val result: Int = job.await()
            println("Task 2 is end, result is $result.")
        }.join()
    }
}

/*
 * 示例：任务调度 - 等待多个任务的结果1。
 */
fun example06() {
    // 测试方法：延时特定秒数，并以该秒数为返回值。
    suspend fun task(name: String, time: Long): Long {
        println("Task $name start. Name:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Name:[${getThread()}] Time:[${getTime()}]")
        return time
    }

    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 使用"async()"方法开启两个异步任务
            val job1: Deferred<Long> = async { task("1", 2000) }
            val job2: Deferred<Long> = async { task("2", 3000) }

            // 使当前任务等待上述两个任务结束，并通过变量接收运行结果。
            val result1 = job1.await()
            val result2 = job2.await()

            println("All task is end, summary is ${result1 + result2}.")
        }.join()
    }
}

/*
 * 示例：任务调度 - 等待多个任务的结果2。
 */
fun example07() {
    // 测试方法：随机延时若干秒。
    suspend fun task(name: String): Int {
        println("Task $name start. Name:[${getThread()}] Time:[${getTime()}]")
        // 随机延时1-5秒
        val time: Int = (Random.nextInt(5) + 1)
        delay(time * 1000L)
        println("Task $name end. Name:[${getThread()}] Time:[${getTime()}]")
        return time
    }

    runBlocking {
        CoroutineScope(Dispatchers.Default).launch {
            // 创建集合保存任务实例
            val jobs: MutableList<Deferred<Int>> = mutableListOf()
            // 循环开启多个任务
            for (i in 1..10) {
                // 启动任务，并将任务实例保存至集合
                jobs.add(async { task("$i") })
            }

            /*
             * 调用"awaitAll()"方法，等待所有任务完成。
             *
             * "awaitAll()"方法的参数是Deferred<T>类型可变参数，"toTypedArray()"方法可以将集合转为Array<T>类型变量，在Array<T>之前
             * 加上"*"可以便捷地将Array<T>转换为多个参数的形式。
             */
            val results: List<Int> = awaitAll(*jobs.toTypedArray())
            println("All task is end, results is ${results}.")
        }.join()
    }
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
