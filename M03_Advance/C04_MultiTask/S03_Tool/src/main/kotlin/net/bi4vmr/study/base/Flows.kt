package net.bi4vmr.study.base

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// val flow = MutableStateFlow(0)
val flow = MutableSharedFlow<Int>(extraBufferCapacity = 100)

val collecterReady = CompletableDeferred<Unit>()

fun main() = runBlocking {
//     CoroutineScope(Dispatchers.IO).launch {
//         flow.collect {
//             println("flow changed: $it")
//         }
//     }
//
//     repeat(10) {
//         flow.value += 1
//     }
    example02()

    // val f = flow {
    //     println("e1 thread name ${Thread.currentThread().name}")
    //     emit(1)
    //
    //     println("e2 thread name ${Thread.currentThread().name}")
    //     emit(2)
    // }
    //
    // val t = CoroutineScope(Dispatchers.Default)
    // t.launch {
    //     f.collect {
    //         println("1 collect thread name ${Thread.currentThread().name}")
    //         println("1 接收到的值：$it")
    //     }
    // }
    //
    //
    // t.launch {
    //     f.collect {
    //         println("2 collect thread name ${Thread.currentThread().name}")
    //         println("2 接收到的值：$it")
    //     }
    // }


    // CoroutineScope(Dispatchers.IO).launch {
    //     collecterReady.complete(Unit)
    //     flow.collect {
    //         println("flow changed: $it")
    //     }
    // }
    //
    // // 等待收集器准备就绪再发出数据
    // runBlocking { collecterReady.await() }
    //
    // repeat(10) {
    //    val i =  flow.tryEmit(1)
    //     println("emit result $i")
    // }

    // Thread.sleep(1000L)
}

fun example01() {
    // 定义Flow
    val flow = flow {
        println("Download start. Thread Name:[${Thread.currentThread().name}]")

        // 模拟下载进度从0至100
        (0..100).forEach { progress ->
            // 模拟下载耗时
            delay(100L)

            // 发送当前进度给接收者
            emit(progress)
        }

        println("Download end. Thread Name:[${Thread.currentThread().name}]")
    }


    // 接收Flow中的数据
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


fun example02() {
    val flow = MutableSharedFlow<Int>()

    // 发送一些数据(1 - 5)
    (1..5).forEach {
        flow.tryEmit(it)
    }

    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    val j = scope.launch {
        println("collect")
        flow.collect {
            println("flow changed: $it")
        }
    }

    runBlocking {
        delay(1000)
    }

    // 发送一些数据(6 - 10)
    (6..10).forEach {
        println("emit: $it")
        // 禁止挂起，所以无缓冲的Flow不能通过该方法发送数据。
        val b = flow.tryEmit(it)
        println("emit result: $b")
    }

    // 测试线程等待协程执行完毕
    runBlocking {
        // // 发送一些数据(6 - 10)
        // (6..10).forEach {
        //     flow.emit(it)
        // }

        j.join()
    }
}
