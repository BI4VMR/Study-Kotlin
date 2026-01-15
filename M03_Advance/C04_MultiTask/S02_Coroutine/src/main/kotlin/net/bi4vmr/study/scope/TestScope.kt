package net.bi4vmr.study.scope

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 示例代码：协程环境。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    // example04_3()

    val job = GlobalScope.launch {
        println("coroutineContext: $coroutineContext")
        delay(100L)
        println("Glo1 end")
    }
    // println()
    // job.cancel()

    // Thread.sleep(1000L)
}

suspend fun ss() {
    // 用于获取调用该挂起函数的作用域
    coroutineScope {
        // 可以获取Context
        coroutineContext
        currentCoroutineContext()

        // 可以开启新的协程
        launch { }
    }
}


/**
 * 示例一：创建协程作用域。
 *
 * 在本示例中，我们创建一些协程作用域，并指定上下文。
 */
fun example01() {
    // 基本的上下文
    val scopeA = CoroutineScope(Dispatchers.Default)
    println("ScopeA: ${scopeA.coroutineContext}")

    // 额外指定名称和SupervisorJob
    val scopeB = CoroutineScope(Dispatchers.IO + CoroutineName("A") + SupervisorJob())
    println("ScopeB: ${scopeB.coroutineContext}")

    // EmptyCoroutineContext
    val scopeC = CoroutineScope(EmptyCoroutineContext)
    println("ScopeC: ${scopeC.coroutineContext}")
}


/**
 * 示例二：修改协程上下文。
 *
 * 在本示例中，我们创建协程作用域，并对上下文进行修改。
 */
fun example02() {
    val scope = CoroutineScope(Dispatchers.IO + CoroutineName("A") + SupervisorJob())
    println("Scope: ${scope.coroutineContext}")

    // 通过KEY获取Context中的元素
    val job = scope.coroutineContext[Job]
    println("获取元素：$job")

    // 替换同KEY元素生成新的Context
    val newContext1 = scope.coroutineContext + Dispatchers.Default
    println("替换元素：$newContext1")

    // 添加元素生成新的Context
    val newContext2 = scope.coroutineContext + CoroutineExceptionHandler { _: CoroutineContext, e: Throwable ->
        println("捕获到异常：${e.message}")
    }
    println("新增元素：$newContext2")

    // 移除元素生成新的Context
    val newContext3 = scope.coroutineContext.minusKey(CoroutineName)
    println("移除元素：$newContext3")
}


/**
 * 示例三：作用域名称。
 *
 * 在本示例中，我们创建协程作用域并指定名称，然后在协程中获取并输出消息。
 */
fun example03() = runBlocking {
    // 未指定CoroutineName的作用域
    CoroutineScope(EmptyCoroutineContext).launch {
        val name = coroutineContext[CoroutineName]?.name
        println("默认的作用域名称：[$name]")
    }.join()

    // 指定了CoroutineName的作用域
    CoroutineScope(CoroutineName("BusinessA")).launch {
        val name = coroutineContext[CoroutineName]?.name
        println("指定的作用域名称：[$name]")
    }.join()
}


/**
 * 示例三：作用域名称。
 *
 * 在本示例中，我们创建协程作用域并指定名称，然后在协程中获取并输出消息。
 */
fun example04() = runBlocking {
    // 创建作用域但未使用SupervisorJob，默认为普通Job。
    val scope = CoroutineScope(Dispatchers.Default)
    val job1 = scope.launch {
        println("Normal task start.")
        delay(100L)
        println("Normal task end.")
    }

    val job2 = scope.launch {
        println("Error task start.")
        delay(50L)
        throw IllegalStateException("Mock Error!")
    }

    job1.join()
    job2.join()
    println("Root task end. Job1 cancelled:[${job1.isCancelled}]")
}

fun example04_2() = runBlocking {
    // 创建作用域且使用SupervisorJob
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val job1 = scope.launch {
        println("Normal task start.")
        delay(100L)
        println("Normal task end.")
    }

    val job2 = scope.launch {
        println("Error task start.")
        delay(50L)
        throw IllegalStateException("Mock Error!")
    }

    job1.join()
    job2.join()
    println("Root task end. Job1 cancelled:[${job1.isCancelled}]")
}

fun example04_3() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught exception: ${exception.message}")
    }
    // 创建作用域且使用SupervisorJob
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + handler)
    val job1 = scope.launch {
        println("Normal task start.")
        delay(100L)

        launch {
            println("Error task start.")
            delay(50L)
            throw IllegalStateException("Mock Error!")
        }.join()

        println("Normal task end.")
    }

    // val job2 = scope.launch {
    //     println("Error task start.")
    //     delay(50L)
    //     throw IllegalStateException("Mock Error!")
    // }

    job1.join()
    // job2.join()
    println("Root task end. Job1 cancelled:[${job1.isCancelled}]")
}

fun example04_44() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught exception: ${exception.message}")
    }
    supervisorScope {

    }
    // 创建作用域且使用SupervisorJob
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + handler)
    val job1 = scope.launch {
        println("Normal task start.")
        delay(100L)
        println("Normal task end.")
    }

    val job2 = scope.launch {
        println("Error task start.")
        delay(50L)
        throw IllegalStateException("Mock Error!")
    }

    job1.join()
    job2.join()
    println("Root task end. Job1 cancelled:[${job1.isCancelled}]")
}


/*
 * 示例：编码风格。
 */

// 不推荐的用法
class UserManager {

    fun updateCurrentUID() {
        CoroutineScope(Dispatchers.IO).launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }

    fun updateCurrentName() {
        CoroutineScope(Dispatchers.IO).launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }
}

// 推荐用法一：由调用者提供协程环境。
class UserManagerV1 {

    suspend fun updateCurrentUID(): String = "1"

    suspend fun updateCurrentName(): String = "User1"
}

// 推荐用法二：调用者无法使用协程，由当前类维护协程环境。
class UserManagerV2(
    // 使用外部传入的调度器，或提供默认的调度器。
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // 所有业务使用统一的作用域
    private val scope = CoroutineScope(dispatcher)

    fun updateCurrentUID() {
        // 方法内部不要单独创建作用域
        scope.launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }

    fun updateCurrentName() {
        // 方法内部不要单独创建作用域
        scope.launch {
            // 模拟耗时操作...
            delay(1000L)
        }
    }

    // 提供组件销毁方法，取消当前作用域的所有协程。
    fun destroy() {
        scope.cancel()
    }
}
