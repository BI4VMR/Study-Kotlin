package net.bi4vmr.study.higherorder

/**
 * 示例代码：高阶函数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example01()
}


/**
 * 示例一：将整数列表中的元素倒序排列。
 *
 * 在本示例中，我们定义一个整数列表，并将其中的元素倒序排列。
 */
fun example01() {
    download("-") { value ->
        println(value)
    }
}

// 定义高阶函数：下载，其中第二参数 `onProgress` 的类型是函数。
fun download(url: String, onProgress: (value: Int) -> Unit) {
    (0..100).forEach { value ->
        // 通过 `invoke()` 方法调用函数，回调当前进度值。
        onProgress.invoke(value)

        // 上述写法可以简化为函数调用
        // onProgress(value)
    }
}

// 定义高阶函数：下载，函数参数为可空类型。
fun download2(url: String, onProgress: ((value: Int) -> Unit)?) {
    // 此时不能写作函数调用，需要检查是否为空再调用 `invoke()` 方法。
    onProgress?.invoke(100)
}
