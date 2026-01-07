package net.bi4vmr.study.cooperation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 测试代码：任务调度。
 *
 * @author bi4vmr@outlook.com
 */
fun main() {
    example05()
}

/**
 * 示例一：顺序执行任务。
 */
fun example01() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Thread:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Thread:[${getThread()}] Time:[${getTime()}]")
    }

    CoroutineScope(Dispatchers.Default).launch {
        // 先执行第一个任务
        task("A", 2000)
        // 第一个任务执行完毕后，再执行第二个任务。
        task("B", 2000)
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例：顺序执行任务 - 协程与接口回调风格对比（伪代码）。
 */
fun example02() {
    /* ----- 接口回调风格 -----

     val studentID: Long = 1

     // 首先根据ID查询学生信息
     queryStudent(studentID, object : OnResult() {
         override fun onSuccess(student: Student) {
             // 获取班级ID
             val classID: Long = student.classID
             // 然后根据班级ID查询班级信息
             queryClass(classID, object : OnResult() {
                 override fun onSuccess(classInfo: ClassInfo) {
                     // 显示班级信息
                     println(classInfo)
                 }
             })
         }
     })
     */

    /* ----- 协程风格 -----

     val studentID: Long = 1

     CoroutineScope(Dispatchers.Default).launch {
         // 首先根据ID查询学生信息
         val student: Student = queryStudent(studentID)
         // 获取班级ID
         val classID: Long = student.classID
         // 然后根据班级ID查询班级信息
         val classInfo: ClassInfo = queryClass(classID)
         // 显示班级信息
         println(classInfo)
     }
     */
}

/**
 * 示例：并发执行任务。
 */
fun example03() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Thread:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Thread:[${getThread()}] Time:[${getTime()}]")
    }

    CoroutineScope(Dispatchers.Default).launch {
        // 创建协程执行任务A
        launch { task("A", 2000) }
        // 创建协程执行任务B
        launch { task("B", 2000) }
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/*
 * 示例：等待其他任务完成。
 */
fun example04() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Time:[${getTime()}]")
    }

    CoroutineScope(Dispatchers.Default).launch {
        println("Task Root start.")
        // 使用"launch()"方法开启子任务。
        val job: Job = launch { task("A", 2000) }

        // 在顶级协程中调用子任务的"join()"方法，等待子任务结束再继续运行。
        job.join()
        println("Task Root end.")
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例：获取其他任务的结果。
 */
fun example05() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Time:[${getTime()}]")
    }

    CoroutineScope(Dispatchers.Default).launch {
        println("Task Root start.")
        // 使用"async()"方法开启任务，并声明变量保存任务对象，以便后续获取返回值。
        val job: Deferred<Int> = async {
            task("A", 2000)
            // 协程体是一个Lambda表达式，最后一条语句的值即任务的返回值。
            114514
        }
        // 异步等待任务结束，并接收返回值。
        val result: Int = job.await()
        println("Task Root end, task A result is $result.")
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例：合并多个任务的结果（方式1）。
 */
fun example06() {
    // 测试方法：延时特定秒数，并以该秒数为返回值。
    suspend fun task(name: String, time: Long): Long {
        println("Task $name start. Thread:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Thread:[${getThread()}] Time:[${getTime()}]")
        return time
    }

    CoroutineScope(Dispatchers.Default).launch {
        // 使用"async()"方法开启两个异步任务
        val job1: Deferred<Long> = async { task("1", 2000) }
        val job2: Deferred<Long> = async { task("2", 3000) }

        // 使当前任务等待上述两个任务结束，并通过变量接收运行结果。
        val result1 = job1.await()
        val result2 = job2.await()

        println("All task is end, summary is ${result1 + result2}.")
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例：合并多个任务的结果（方式2）。
 */
fun example07() {
    // 测试方法：随机延时若干秒。
    suspend fun task(name: String): Int {
        println("Task $name start. Thread:[${getThread()}] Time:[${getTime()}]")
        // 随机延时1-5秒
        val time: Int = (Random.nextInt(5) + 1)
        delay(time * 1000L)
        println("Task $name end. Thread:[${getThread()}] Time:[${getTime()}]")
        return time
    }

    CoroutineScope(Dispatchers.Default).launch {
        // 创建集合保存Deferred对象
        val jobs: MutableList<Deferred<Int>> = mutableListOf()
        // 循环开启多个任务
        for (i in 1..5) {
            // 启动任务，并将Deferred对象保存至集合。
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
    }

    // 阻塞主线程6秒，避免协程提前终止。
    Thread.sleep(6000L)
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
