package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 示例：SharedFlow。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() = example02()


/**
 * 示例二： SharedFlow 的基本应用。
 *
 * 在本示例中，我们定义一个Flow通告事件序列，然后接收事件并显示在控制台上。
 */
fun example02() {
    // 定义可写入的 SharedFlow
    val sharedFlow = MutableSharedFlow<Int>()

    // 发送一些数据(1 - 3)
    runBlocking {
        (1..3).forEach {
            println("主线程发送数据：[$it]")
            sharedFlow.emit(it)
        }
    }


    // 开启协程接收 SharedFlow 中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        // 调用 `collect` 方法监听Flow中的数据
        sharedFlow.collect { value ->
            // 每当新数据到达时，该语句被执行一次。
            println("Flow change. Value:[$value]")
        }
    }


    runBlocking {
        // 测试线程等待接收线程启动再开始发送数据
        delay(250)

        // 发送一些数据(4 - 6)
        (4..6).forEach {
            println("主线程发送数据：[$it]")
            sharedFlow.emit(it)
        }

        // 测试线程等待接收线程处理完毕再结束整个程序
        delay(250)
    }
}
