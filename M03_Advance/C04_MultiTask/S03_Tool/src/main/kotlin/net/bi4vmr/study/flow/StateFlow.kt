package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * 示例：StateFlow。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example03()
}


/**
 * 示例三： StateFlow 的基本应用。
 *
 * 在本示例中，我们定义 StateFlow 用于维护某个功能的开关状态。
 */
fun example03() {
    // 定义 StateFlow ，用于管理开关状态，初始值为 `false` 。
    val stateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // 可以访问 `value` 属性获取 StateFlow 容器中当前的值。
    println("当前的值：${stateFlow.value}")

    // 创建协程监听 StateFlow 中的消息
    val scope = CoroutineScope(Dispatchers.IO)
    val listenJob = scope.launch {
        // 调用 `collect` 方法监听Flow中的数据
        stateFlow.collect { value ->
            println("监听协程收到消息：$value")
        }
    }


    // 连续变化测试
    runBlocking {
        // 测试线程等待接收协程启动再开始发送数据
        delay(250.milliseconds)

        // 更新状态
        println("测试线程发送状态：`true`")
        stateFlow.value = true
        println("测试线程发送状态：`false`")
        stateFlow.value = false
        println("测试线程发送状态：`true`")
        stateFlow.value = true
    }


    // 测试线程等待接收协程处理完毕再结束整个程序
    runBlocking {
        delay(250.milliseconds)
        listenJob.cancel()
    }
}


data class Student(
    var id: String = "",
    var name: String = "",
    var age: Int = 0
)


fun a() {
    val initData = Student("1", "张三", 20)
    // 定义可写入的StateFlow，初始值为 `100` 。
    val stateFlow: MutableStateFlow<Student> = MutableStateFlow(initData)


    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        // 调用 `collect` 方法监听Flow中的数据
        stateFlow.collect { value ->
            // 每当新数据到达时，该语句被执行一次。
            println("Flow change. Value:[$value]")
        }
    }


    runBlocking {
        // 测试线程等待接收线程启动再开始发送数据
        delay(250)

        // 直接修改 Flow 容器中对象的属性
        initData.name = "李四"
        // 使用原对象更新 Flow
        stateFlow.value = initData

        // 测试线程等待接收线程处理完毕再结束整个程序
        delay(250)
    }
}
