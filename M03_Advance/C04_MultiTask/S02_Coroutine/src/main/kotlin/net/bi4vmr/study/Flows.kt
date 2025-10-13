package net.bi4vmr.study

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val flow = MutableStateFlow(111)

var count = 0
val mutex: Mutex = Mutex()

fun main() {
    // CoroutineScope(Dispatchers.Default).launch {
    //     delay(1000L)
    //     flow.value = 111
    //     delay(1000L)
    //     flow.emit(111)
    // }
    //
    // CoroutineScope(Dispatchers.Default).launch {
    //     flow.collect {
    //         println("$it")
    //     }
    // }
    //
    // runBlocking {
    //     delay(5000L)
    // }

    suspend fun change(caller: String) {
        delay(10L)
        mutex.withLock {
            count++
            println("$caller -> $count  T:[${Thread.currentThread().name}]")
        }

    }

    runBlocking {
        CoroutineScope(Dispatchers.IO).launch {
            repeat(10) {
                change("A")
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            repeat(10) {
                change("B")
            }
        }

        delay(3000L)
    }
}
