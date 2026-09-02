package net.bi4vmr.study.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
 * 在本示例中，我们定义一个Flow通告状态，然后接收状态并显示在控制台上。
 */
fun example03() {
    // 定义可写入的StateFlow，初始值为 `100` 。
    val flow = MutableStateFlow(100)

    // 可以访问 `value` 属性获取当前的值
    println("初始值：${flow.value}")

    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        // 调用 `collect` 方法监听Flow中的数据
        flow.collect { value ->
            // 每当新数据到达时，该语句被执行一次。
            println("Flow change. Value:[$value]")
        }
    }


    runBlocking {
        // 测试线程等待接收线程启动再开始发送数据
        delay(250)

        // 发送一些数据(1 - 3)
        (1..3).forEach {
            println("主线程发送数据：[$it]")
            flow.emit(it)
        }

        delay(250)

        // 再发送一次当前的值
        val current = flow.value
        println("主线程发送数据：[$current]")
        flow.value = current

        // 测试线程等待接收线程处理完毕再结束整个程序
        delay(250)
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
