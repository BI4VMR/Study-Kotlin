package net.bi4vmr.study.workwiththread

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 示例代码：线程交互。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example05()
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
 * 示例一：接口回调风格的API。
 *
 * 在本示例中，我们定义一个接口回调风格的模拟网络请求API，并演示它的调用方法。
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
 * 示例二：协程风格的API。
 *
 * 在本示例中，我们使用 `suspendCoroutine()` 方法将回调接口 `request()` 转换为挂起函数。
 */
fun example02() {
    runBlocking {
        println("Mock request start. Time:[${getTime()}]")
        // 声明变量以便接收请求成功的结果
        val data = requestSuspend(true)
        println("Request success. Time:[${getTime()}] Data:[$data]")
    }
}

/**
 * 示例三：反馈异常状态。
 *
 * 在本示例中，我们通过捕获 `requestSuspend()` 方法可能发生的异常，处理请求失败事件。
 */
fun example03() {
    runBlocking {
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
 * 示例四：处理中断请求。
 *
 * 在本示例中，我们为前文“示例二”中的 `requestSuspend()` 方法添加中断处理功能。
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
 * 示例五：将协程API转换为接口回调API。
 *
 * 在本示例中，我们使用 `runBlocking()` 方法将前文“示例二”中的挂起函数 `requestSuspend()` 转换为接口回调形式。
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
