package net.bi4vmr.study.workwiththread

import kotlinx.coroutines.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 测试代码 - 与线程交互。
 *
 * @author BI4VMR。
 */
fun main() {
    example06()
}

/**
 * 网络请求回调接口。
 */
interface NetCallback {

    // 请求成功
    fun onSuccess(data: String)

    // 请求失败
    fun onFailure(message: String)
}

/**
 * 模拟网络请求。
 *
 * @param[result] 控制请求结果。
 * @param[callback] 结果回调。
 */
fun request(result: Boolean, callback: NetCallback) {
    // 开启新线程进行网络请求
    thread {
        // 线程休眠，模拟网络延时。
        Thread.sleep(3000L)
        // 休眠结束，调用回调方法反馈执行结果。
        if (result) {
            callback.onSuccess("200 - OK.")
        } else {
            callback.onFailure("502 - Bad Gateway.")
        }
    }
}

/**
 * 示例：使用接口回调风格的API。
 */
fun example01() {
    println("Mock request start. Time:[${getTime()}]")
    request(true, object : NetCallback {
        override fun onSuccess(data: String) {
            println("OnSuccess. Time:[${getTime()}] Data:[$data]")
        }

        override fun onFailure(message: String) {
            println("OnFailure. Time:[${getTime()}] Info:[$message]")
        }
    })
}

/**
 * 模拟网络请求（挂起函数实现）。
 *
 * @param[result] 控制请求结果。
 */
private suspend fun requestSuspend(result: Boolean): String {
    return suspendCoroutine {
        request(result, object : NetCallback {
            override fun onSuccess(data: String) {
                // 请求成功，解除挂起状态并返回数据。
                it.resume(data)
            }

            override fun onFailure(message: String) {
                // 请求失败，抛出异常。
                val exception = Exception(message)
                it.resumeWithException(exception)
            }
        })
    }
}

/**
 * 示例：使用协程风格的API（请求成功）。
 */
fun example02() {
    CoroutineScope(Dispatchers.IO).launch {
        println("Mock request start. Time:[${getTime()}]")
        // 声明变量以便接收请求成功的结果
        val data = requestSuspend(true)
        println("Request success. Time:[${getTime()}] Data:[$data]")
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 示例：使用协程风格的API（请求失败）。
 */
fun example03() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            println("Mock request start. Time:[${getTime()}]")
            // 模拟请求失败的情况
            val data = requestSuspend(false)
            println("Request success. Time:[${getTime()}] Data:[$data]")
        } catch (e: Exception) {
            // 捕获异常以获取失败详情
            println("Request failure. Time:[${getTime()}] Info:[${e.message}]")
        }
    }

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 模拟网络请求（挂起函数实现-可取消）。
 *
 * @param[result] 控制请求结果。
 */
private suspend fun requestSuspend2(result: Boolean): String {
    return suspendCancellableCoroutine {
        // 此处可以书写收到中断请求后的清理工作
        it.invokeOnCancellation {
            println("Task was canceled!")
        }

        request(result, object : NetCallback {
            override fun onSuccess(data: String) {
                // 请求成功，解除挂起状态并返回数据。
                it.resume(data)
            }

            override fun onFailure(message: String) {
                // 请求失败，抛出异常。
                val exception = Exception(message)
                it.resumeWithException(exception)
            }
        })
    }
}

/**
 * 示例：模拟网络请求（中途取消任务）。
 */
fun example04() {
    val job: Job = CoroutineScope(Dispatchers.IO).launch {
        try {
            println("Mock request start. Time:[${getTime()}]")
            // 模拟请求失败的情况
            val data = requestSuspend2(true)
            println("Request success. Time:[${getTime()}] Data:[$data]")
        } catch (e: Exception) {
            // 捕获异常以获取失败详情
            println("Request failure. Time:[${getTime()}] Info:[${e.message}]")
        }
    }

    // 延时1秒后取消协程任务
    Thread.sleep(1000L)
    job.cancel(CancellationException("Task has been canceled."))

    // 阻塞主线程5秒，避免协程提前终止。
    Thread.sleep(5000L)
}

/**
 * 模拟网络请求（协程转接口回调）。
 *
 * @param[result] 控制请求结果。
 * @param[callback] 结果回调。
 */
fun requestCallback(result: Boolean, callback: NetCallback) {
    // 在新线程进行网络请求
    thread {
        // 开启协程任务并阻塞当前线程
        runBlocking {
            try {
                val data: String = requestSuspend(result)
                // 获取到结果后，通过回调方法通知调用者。
                callback.onSuccess(data)
            } catch (e: Exception) {
                callback.onFailure(e.message ?: "")
            }
        }
    }
}

/**
 * 示例：将协程API转换为接口回调风格的API。
 */
fun example05() {
    println("Mock request start. Time:[${getTime()}]")
    requestCallback(true, object : NetCallback {
        override fun onSuccess(data: String) {
            println("OnSuccess. Time:[${getTime()}] Data:[$data]")
        }

        override fun onFailure(message: String) {
            println("OnFailure. Time:[${getTime()}] Info:[$message]")
        }
    })
}

/**
 * 示例：开启一个协程，使主线程等待协程执行完毕再结束。
 */
fun example06() {
    runBlocking {
        // 启动协程后调用"join()"方法，使主线程等待协程执行完毕。
        CoroutineScope(Dispatchers.Default).launch {
            println("Task start.")
            delay(2000)
            println("Task end.")
        }.join()
    }
    println("Main thread end.")
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
