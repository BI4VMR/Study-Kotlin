package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * 示例：Flow。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例一： Flow 的基本应用。
 *
 * 在本示例中，我们定义并使用 Flow ，模拟文件下载过程。
 */
fun example01() {
    /* 定义 Flow */
    val flow: Flow<Int> = flow {
        println("Download start. Thread Name:[${Thread.currentThread().name}]")

        // 模拟下载进度从 0 至 100
        (0..100).forEach { progress ->
            // 模拟下载耗时
            delay(10L.milliseconds)

            // 发送当前进度给接收者
            emit(progress)
        }

        println("Download end. Thread Name:[${Thread.currentThread().name}]")
    }


    /* 使用 Flow */
    val scope = CoroutineScope(Dispatchers.IO)
    // 接收者 A
    val jobA = scope.launch {
        flow.collect {
            println("Progress change. Value:[$it] Thread Name:[${Thread.currentThread().name}]")
        }
    }
    // 接收者 B
    val jobB = scope.launch {
        flow.collect {
            println("Progress change. Value:[$it] Thread Name:[${Thread.currentThread().name}]")
        }
    }


    // 测试线程等待协程执行完毕
    runBlocking {
        jobA.join()
        jobB.join()
    }
}
