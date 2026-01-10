package net.bi4vmr.study.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 示例代码：协程基础。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example04()
}


/**
 * 示例一：创建基本的协程。
 *
 * 在本示例中，我们创建一个协程，模拟耗时任务并在控制台上显示相关信息。
 */
fun example01() {
    /*
     * 使用Default调度器，提交任务并启动协程。
     */
    CoroutineScope(Dispatchers.Default).launch {
        println("Task start. Thread:[${getThread()}] Time:[${getTime()}]")
        // 延时2秒，模拟耗时操作。
        delay(2000)
        println("Task end. Thread:[${getThread()}] Time:[${getTime()}]")
    }

    /*
     * 阻塞主线程5秒，避免协程提前终止。
     *
     * 若在Web Server、Android等主线程无限循环的环境中实验，无需调用"Thread.sleep()"方法。
     */
    Thread.sleep(5000L)
}

/**
 * 示例二：挂起函数。
 *
 * 在本示例中，我们定义一个挂起函数，并在协程环境中调用它。
 */
fun example02() {
    // 定义挂起函数
    suspend fun task(): Int {
        println("Task start. Thread:[${getThread()}] Time:[${getTime()}]")
        delay(2000)
        println("Task end. Thread:[${getThread()}] Time:[${getTime()}]")
        return 0
    }

    CoroutineScope(Dispatchers.Default).launch {
        // 在协程环境中调用挂起函数，并获取返回值。
        val value = task()
        println("Task return value is $value.")
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例三：挂起与恢复机制。
 *
 * 在本示例中，我们同时开启多个协程，测试协程任务与工作线程之间的关系。
 */
fun example03() {
    // 挂起函数示例
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Thread:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Thread:[${getThread()}] Time:[${getTime()}]")
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

/**
 * 示例四：协程的生命周期。
 *
 * 在本示例中，我们创建一个协程，并尝试访问协程任务的三个公开属性。
 */
fun example04() {
    // 启动协程任务，并声明变量保存Job对象。
    val job: Job = CoroutineScope(Dispatchers.Default).launch {
        println("Task start. Time:[${getTime()}]")
        delay(2000L)
        println("Task end. Time:[${getTime()}]")
    }

    // 主线程等待100毫秒，然后访问属性获取协程任务的状态。
    Thread.sleep(100L)
    // 判断当前任务是否为Active状态
    val active: Boolean = job.isActive
    // 判断当前任务是否为Completed状态
    val completed: Boolean = job.isCompleted
    // 判断当前任务是否为Cancelled状态
    val cancelled: Boolean = job.isCancelled
    println("正在运行:[$active] 任务完成:[$completed] 任务取消:[$cancelled]")

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例五：中断未挂起的任务。
 *
 * 在本示例中，我们创建一个具有循环任务的协程，并在主线程发起中断请求。
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

/**
 * 示例六：中断已挂起的任务。
 *
 * 在本示例中，我们创建一个协程，并在任务进入挂起状态时，从主线程发起中断请求。
 */
fun example06() {
    // 启动一个协程，循环输出日志信息。
    val job: Job = CoroutineScope(Dispatchers.Default).launch {
        try {
            println("Task start. Time:[${getTime()}]")
            delay(2000L)
            println("Task end.")
        } catch (e: CancellationException) {
            println("Catch cancellation exception! Time:[${getTime()}]")
        } finally {
            println("Do some clean work...")
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
