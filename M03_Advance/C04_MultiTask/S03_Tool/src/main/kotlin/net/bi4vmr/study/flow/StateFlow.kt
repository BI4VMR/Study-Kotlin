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
    example06()
}


/*
 * 示例四： StateFlow 的基本应用。
 *
 * 在本示例中，我们定义 StateFlow 用于维护某个功能的开关状态。
 */
fun example04() {
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


/*
 * 示例五：数据更新。
 *
 * 在本示例中，我们了解 StateFlow 的常见错误用法，并将其改正。
 */

// 数据实体类
data class Student(
    val id: String = "",
    var name: String = "",
    var age: Int = 0
)


fun example05() {
    val initData = Student("1", "张三", 20)
    // 定义 StateFlow ，初始值为 `initData` 。
    val stateFlow: MutableStateFlow<Student> = MutableStateFlow(initData)


    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    val listenJob = scope.launch {
        stateFlow.collect { value ->
            println("监听协程收到消息：$value")
        }
    }


    /* 错误示范：直接修改原对象的属性 */
    runBlocking {
        // 测试线程等待接收线程启动再开始发送数据
        delay(250.milliseconds)
    }

    // 直接修改 Flow 容器中对象的属性
    initData.age = 21

    // 读取当前 Flow 容器中的对象
    println("Flow 当前存储的状态：${stateFlow.value}")

    // 使用原对象更新 Flow
    println("测试线程更新状态（原对象）：$initData")
    stateFlow.value = initData


    /* 正确示范：创建新对象并提交更新 */
    // 创建新对象，指明需要更新的属性，并复制其他属性。
    val newData = initData.copy(age = 22)

    // 使用新对象更新 Flow
    println("测试线程更新状态（新对象）：$newData")
    stateFlow.value = newData


    // 测试线程等待接收协程处理完毕再结束整个程序
    runBlocking {
        delay(250.milliseconds)
        listenJob.cancel()
    }
}


/*
 * 示例六：列表更新。
 *
 * 在本示例中，我们了解 StateFlow 内容为列表时的更新方式。
 */
fun example06() {
    val initList: MutableList<Student> = mutableListOf(
        Student("1", "张三", 20),
        Student("2", "李四", 21)
    )
    val stateFlow: MutableStateFlow<List<Student>> = MutableStateFlow(initList)


    // 开启协程接收Flow中的数据
    val scope = CoroutineScope(Dispatchers.IO)
    val listenJob = scope.launch {
        stateFlow.collect { list ->
            println("----- 监听协程收到消息，列表长度：${list.size} -----")
            list.forEach { student -> println("$student") }
            println("----- 监听协程收到消息，完毕。 -----")
        }
    }


    /* 错误示范：直接修改原列表 */
    runBlocking {
        // 测试线程等待接收线程启动再开始发送数据
        delay(250.milliseconds)
    }

    // 直接修改 Flow 容器中的列表项
    initList[1].name = "李田所"
    initList[1].age = 24

    // 使用原列表更新 Flow
    println("测试线程更新状态（原列表）：$initList")
    stateFlow.value = initList


    /* 正确示范：创建新列表与新对象并提交更新 */
    // 创建新列表
    val newList = initList.toMutableList()
    // 创建新对象，替换列表中的旧数据。
    val newData = newList[1].copy(age = 25)
    newList[1] = newData

    // 使用新对象更新 Flow
    println("测试线程更新状态（新列表）：$newList")
    stateFlow.value = newList


    // 测试线程等待接收协程处理完毕再结束整个程序
    runBlocking {
        delay(250.milliseconds)
        listenJob.cancel()
    }
}
