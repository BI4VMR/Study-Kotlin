package net.bi4vmr.study.base

import kotlinx.coroutines.*

/**
 * 测试代码 - 基本应用。
 */
fun main() {
    runBlocking {
        example02()
    }
}

/*
 * 示例：空值安全
 */
fun example01() {
    CoroutineScope(Dispatchers.Main).launch {

    }
}

/*
 * 示例：延时
 */
suspend fun example02() {
    CoroutineScope(Dispatchers.Default).launch {
        println("Coroutine exec start.")
        delay(3000)
        println("Coroutine exec end.")
    }.join()
}
