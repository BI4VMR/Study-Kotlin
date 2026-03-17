package net.bi4vmr.study.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * 示例代码：数据同步。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例一：创建协程作用域。
 *
 * 在本示例中，我们创建一些协程作用域，并指定上下文。
 */
var i = 0
fun example01() {
    // val m: Mutex = Mutex()
    repeat(100) {
        CoroutineScope(Dispatchers.IO).launch {
            // m.withLock {
            i++
            // }
        }
    }
    println("i:$i")
}
