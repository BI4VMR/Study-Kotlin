package net.bi4vmr.study.flow

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val collecterReady = CompletableDeferred<Unit>()

fun main() = example01()


/**
 * 示例一：Flow的基本应用。
 *
 * 在本示例中，我们定义一个Flow实例，模拟下载文件的过程，然后接收下载进度并显示到控制台上。
 */
fun example01() {
    // 定义Flow
    val flow: Flow<Int> = flow {
        println("Download start. Thread Name:[${Thread.currentThread().name}]")

        // 模拟下载进度从0至100
        (0..100).forEach { progress ->
            // 模拟下载耗时
            delay(10L)

            // 发送当前进度给接收者
            emit(progress)
        }

        println("Download end. Thread Name:[${Thread.currentThread().name}]")
    }


    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    val job1 = scope.launch {
        flow.collect {
            println("Download progress change. Value:[$it] Thread Name:[${Thread.currentThread().name}]")
        }
    }

    val job2 = scope.launch {
        flow.collect {
            println("Download progress change. Value:[$it] Thread Name:[${Thread.currentThread().name}]")
        }
    }


    // 测试线程等待协程执行完毕
    runBlocking {
        job1.join()
        job2.join()
    }
}


/**
 * 示例二：SharedFlow的基本应用。
 *
 * 在本示例中，我们定义一个Flow通告事件序列，然后接收事件并显示在控制台上。
 */
fun example02() {
    // 定义可写入的SharedFlow
    val flow = MutableSharedFlow<Int>()

    // 发送一些数据(1 - 3)
    runBlocking {
        (1..3).forEach {
            println("主线程发送数据：[$it]")
            flow.emit(it)
        }
    }


    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        // 调用 `collect` 方法监听Flow中的数据
        flow.collect { value ->
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
            flow.emit(it)
        }

        // 测试线程等待接收线程处理完毕再结束整个程序
        delay(250)
    }
}


/**
 * 示例三：StateFlow的基本应用。
 *
 * 在本示例中，我们定义一个Flow通告状态，然后接收状态并显示在控制台上。
 */
fun example03() {
    // 定义可写入的StateFlow，初始值为 `100` 。
    val flow = MutableStateFlow(100)

    // 可以访问 `value` 属性获取当前的值
    println("初始值：${flow.value}")

    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        // 调用 `collect` 方法监听Flow中的数据
        flow.collect { value ->
            // 每当新数据到达时，该语句被执行一次。
            println("Flow change. Value:[$value]")
        }
    }


    runBlocking {
        // 测试线程等待接收线程启动再开始发送数据
        delay(250)

        // 发送一些数据(1 - 3)
        (1..3).forEach {
            println("主线程发送数据：[$it]")
            flow.emit(it)
        }

        delay(250)

        // 再发送一次当前的值
        val current = flow.value
        println("主线程发送数据：[$current]")
        flow.value = current

        // 测试线程等待接收线程处理完毕再结束整个程序
        delay(250)
    }
}
