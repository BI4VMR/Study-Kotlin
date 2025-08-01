package net.bi4vmr.study

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val flow = MutableStateFlow(1)

fun main() {
    CoroutineScope(Dispatchers.Default).launch {
        delay(1000L)
        flow.value = 111
    }

    CoroutineScope(Dispatchers.Default).launch {
        flow.collect {
            println("$it")
        }
    }
}
