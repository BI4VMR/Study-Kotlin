package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * 示例：SharedFlow 。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example02()
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
