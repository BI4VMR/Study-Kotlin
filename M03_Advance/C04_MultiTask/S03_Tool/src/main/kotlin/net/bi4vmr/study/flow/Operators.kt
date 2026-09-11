package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * 示例：操作符。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example0111()
}

fun example0111() {
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
        flow
            .onStart {
                println("OnStart.")
            }
            .onEach {
                println("OnEach.")
            }
            .onEmpty {
                println("OnEmpty.")
            }
            .collect {
                println("Progress change. Value:[$it] Thread Name:[${Thread.currentThread().name}]")
            }
    }

    // 测试线程等待协程执行完毕
    runBlocking {
        jobA.join()
    }
}
