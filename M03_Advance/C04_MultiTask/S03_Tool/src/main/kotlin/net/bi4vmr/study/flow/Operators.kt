package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
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

// callback转flow，适用于多次回调接口的场景，例如下载文件进度。
val d = callbackFlow {
    // 回调接口实现
    (0..100).forEach { progress ->
        // 模拟下载耗时
        delay(10L.milliseconds)

        // 发送当前进度给接收者
        val i = trySend(progress)
        i.isSuccess
        // 如果接收者处理缓慢，则会阻塞当前线程，直到接收者处理完毕。
        // trySendBlocking(progress)
    }

    // 注册监听

    // 关闭回调（内容可以为空但不可省略。）
    awaitClose {
        // 注销监听
    }
}

// stateIn 或 sharedIn 将冷流或热流转为对应的类型，可以执行数据转换等操作，便于多处使用。
val b = d.stateIn(
    CoroutineScope(Dispatchers.IO),
    // 5秒后没有订阅者则停止发送数据，避免资源浪费。
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = 0
)

fun example0111() {
    /* 定义 Flow */
    runBlocking {
        b.collect {
            println("P[$it]")
        }
    }
    // val flow: Flow<Int> = flow {
    //     println("Download start. Thread Name:[${Thread.currentThread().name}]")
    //
    //     // 模拟下载进度从 0 至 100
    //     (0..100).forEach { progress ->
    //         // 模拟下载耗时
    //         delay(10L.milliseconds)
    //
    //         // 发送当前进度给接收者
    //         emit(progress)
    //     }
    //
    //     println("Download end. Thread Name:[${Thread.currentThread().name}]")
    // }
    //
    // /* 使用 Flow */
    // val scope = CoroutineScope(Dispatchers.IO)
    // // 接收者 A
    // val jobA = scope.launch {
    //     flow
    //         .onStart {
    //             println("OnStart.")
    //         }
    //         .onEach {
    //             println("OnEach.")
    //         }
    //         .onEmpty {
    //             println("OnEmpty.")
    //         }
    //         .collect {
    //             println("Progress change. Value:[$it] Thread Name:[${Thread.currentThread().name}]")
    //         }
    // }
}
