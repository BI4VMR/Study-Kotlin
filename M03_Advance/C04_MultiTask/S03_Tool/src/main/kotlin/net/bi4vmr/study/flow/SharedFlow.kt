package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

/**
 * 示例：SharedFlow 。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example03()
}


/**
 * 示例二： SharedFlow 的基本应用。
 *
 * 在本示例中，我们定义 SharedFlow 用于通告事件消息。
 */
fun example02() {
    // 定义 SharedFlow ，用于通告事件消息。
    val sharedFlow: MutableSharedFlow<String> = MutableSharedFlow()


    // 发送一些消息
    runBlocking {
        println("测试线程发送消息：正在初始化...")
        sharedFlow.emit("正在初始化...")
        println("测试线程发送消息：初始化成功！")
        sharedFlow.emit("初始化成功！")
    }


    // 创建协程监听 SharedFlow 中的消息
    val scope = CoroutineScope(Dispatchers.IO)
    val listenJob = scope.launch {
        // 调用 `collect()` 方法监听 SharedFlow 中的数据
        sharedFlow.collect { value ->
            println("监听协程收到消息：$value")
        }
        // 热流的 `collect()` 后不能放置任何语句！
    }


    // 再次发送一些消息
    runBlocking {
        // 测试线程等待接收协程启动再发送消息
        delay(250.milliseconds)

        // 发送一些消息
        println("测试线程发送消息：【文件一】下载完成！")
        sharedFlow.emit("【文件一】下载完成！")
        println("测试线程发送消息：【文件二】下载完成！")
        sharedFlow.emit("【文件二】下载完成！")
    }


    // 测试线程等待接收协程处理完毕再结束整个程序
    runBlocking {
        delay(250.milliseconds)
        listenJob.cancel()
    }
}


/**
 * 示例三： SharedFlow 的缓存控制。
 *
 * 在本示例中，我们测试 SharedFlow 的缓存策略，了解它们的行为差异。
 */
fun example03() {
    // 定义 SharedFlow ，用于通告事件消息。
    val sharedFlow: MutableSharedFlow<Int> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )


    // 发送一些消息
    runBlocking {
        println("测试线程发送消息： [1]")
        sharedFlow.emit(1)
        println("测试线程发送消息： [2]")
        sharedFlow.emit(2)
    }


    // 创建协程监听 SharedFlow 中的消息
    val scope = CoroutineScope(Dispatchers.IO)
    val listenJob = scope.launch {
        println("监听协程注册回调")
        sharedFlow.collect { value ->
            println("监听协程收到消息： [$value]")
            delay(1000.milliseconds)
            println("监听协程处理消息： [$value] 完毕！")
        }
    }

    runBlocking {
        // 测试线程等待接收协程启动再发送消息
        delay(250.milliseconds)

        sharedFlow.emit(3)
        println("测试线程发送消息： [3] 完毕，当前时间：[${getTime()}]")
        sharedFlow.emit(4)
        println("测试线程发送消息： [4] 完毕，当前时间：[${getTime()}]")
        sharedFlow.emit(5)
        println("测试线程发送消息： [5] 完毕，当前时间：[${getTime()}]")
    }

    // 测试线程等待接收协程处理完毕再结束整个程序
    runBlocking {
        delay(3500.milliseconds)
        listenJob.cancel()
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
