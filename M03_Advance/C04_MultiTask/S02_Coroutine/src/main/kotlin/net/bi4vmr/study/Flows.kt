package net.bi4vmr.study

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// val flow = MutableStateFlow(0)
val flow = MutableSharedFlow<Int>(extraBufferCapacity = 100)

val collecterReady = CompletableDeferred<Unit>()

fun main() {
//     CoroutineScope(Dispatchers.IO).launch {
//         flow.collect {
//             println("flow changed: $it")
//         }
//     }
//
//     repeat(10) {
//         flow.value += 1
//     }

    CoroutineScope(Dispatchers.IO).launch {
        collecterReady.complete(Unit)
        flow.collect {
            println("flow changed: $it")
        }
    }

    // 等待收集器准备就绪再发出数据
    runBlocking { collecterReady.await() }

    repeat(10) {
       val i =  flow.tryEmit(1)
        println("emit result $i")
    }

    Thread.sleep(1000L)
}
