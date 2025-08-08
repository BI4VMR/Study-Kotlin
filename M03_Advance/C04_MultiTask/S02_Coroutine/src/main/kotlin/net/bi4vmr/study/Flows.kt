package net.bi4vmr.study

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val flow = MutableStateFlow(111)

fun main() {
    CoroutineScope(Dispatchers.Default).launch {
        delay(1000L)
        flow.value = 111
        delay(1000L)
        flow.emit(111)
    }

        CoroutineScope(Dispatchers.Default).launch {
            flow.collect {
                println("$it")
            }
        }

    runBlocking {
        delay(5000L)
    }
}
