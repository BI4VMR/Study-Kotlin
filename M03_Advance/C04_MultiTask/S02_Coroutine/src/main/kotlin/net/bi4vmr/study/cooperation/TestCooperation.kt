package net.bi4vmr.study.cooperation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 测试代码：任务调度。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example08()
}


/**
 * 示例一：串行任务。
 *
 * 在本示例中，我们创建一个协程环境，并观察其中语句的执行顺序。
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
 * 示例二：协程与接口回调代码风格对比（伪代码）。
 *
 * 在本示例中，我们以学生信息管理系统为例，通过伪代码比较协程与接口回调两种编码风格。
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
 * 示例三：并行任务。
 *
 * 在本示例中，我们创建一个协程环境，并通过协程构建器 `launch()` 方法开启两个并行任务。
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

/**
 * 示例四：等待任务完成。
 *
 * 在本示例中，我们创建协程环境，开启一个子协程，然后使顶级协程等待子协程执行完毕，再继续运行。
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
        // 使用 `launch()` 方法开启子任务。
        val job: Job = launch { task("A", 2000) }

        // 在顶级协程中调用子任务的 `join()` 方法，等待子任务结束再继续运行。
        job.join()
        println("Task Root end.")
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例五：获取任务结果。
 *
 * 在本示例中，我们创建协程环境，开启一个子协程，然后使顶级协程等待子协程执行完毕，并获取子协程的结果。
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
        // 使用 `async()` 方法开启任务，并声明变量保存任务对象，以便后续获取返回值。
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
 * 示例六：合并任务结果（方式一）。
 *
 * 在本示例中，我们通过 `async()` 方法开启两个并行任务，并使顶级协程等待它们完成，最后合并二者的结果。
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
        // 使用 `async()` 方法开启两个异步任务
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
 * 示例七：合并任务结果（方式二）。
 *
 * 在本示例中，我们开启5个具有随机执行时长的并行任务，并使顶级协程等待它们完成，最终向控制台输出它们的结果。
 */
fun example07() {
    // 测试方法：随机延时若干秒
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
         * 调用 `awaitAll()` 方法，等待所有任务完成。
         *
         * `awaitAll()` 方法的参数是Deferred<T>类型可变参数， `toTypedArray()` 方法可以将集合转为Array<T>类型变量，在Array<T>之前
         * 加上 `*` 可以便捷地将Array<T>转换为多个参数的形式。
         */
        val results: List<Int> = awaitAll(*jobs.toTypedArray())
        println("All task is end, results is ${results}.")
    }

    // 阻塞主线程6秒，避免协程提前终止。
    Thread.sleep(6000L)
}

/**
 * 示例八：阻塞当前线程。
 *
 * 在本示例中，我们通过 `runBlocking()` 方法提供协程环境，并阻塞测试代码线程，直到协程任务完成再唤醒线程。
 */
fun example08() {
    println("Thread start. Info:[${getThread()}]")

    // 创建协程环境，并阻塞当前线程，直到所有语句执行完毕。
    runBlocking {
        println("Coroutine task start. Time:[${getTime()}]")
        delay(2000)
        println("Coroutine task end. Time:[${getTime()}]")
    }

    println("Thread end. Info:[${getThread()}]")
}

fun example09() {
    // 测试方法：延时特定秒数。
    suspend fun task(name: String, time: Long) {
        println("Task $name start. Thread:[${getThread()}] Time:[${getTime()}]")
        delay(time)
        println("Task $name end. Thread:[${getThread()}] Time:[${getTime()}]")
    }

    // 将CoroutineScope更换为 `runBlocking()` 方法，自动阻塞与唤醒测试线程。
    // CoroutineScope(Dispatchers.Default).launch {
    runBlocking {
        // 先执行第一个任务
        task("A", 2000)
        // 第一个任务执行完毕后，再执行第二个任务。
        task("B", 2000)
    }

    // 不再需要通过固定的延时等待协程完成
    // Thread.sleep(5000L)
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
