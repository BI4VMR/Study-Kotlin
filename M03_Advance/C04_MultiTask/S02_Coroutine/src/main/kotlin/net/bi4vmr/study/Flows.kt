package net.bi4vmr.study

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
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


    flow {
        println("e1 thread name ${Thread.currentThread().name}")
        emit(1)

        println("e2 thread name ${Thread.currentThread().name}")
        emit(2)
    }.collect {
        println("collect thread name ${Thread.currentThread().name}")
        println(it)
    }

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
    //
    // Thread.sleep(1000L)
}
