package net.bi4vmr.study.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 测试代码 - 基本应用。
 */
fun main() {
    example01()
}

/*
 * 示例：空值安全
 */
fun example01() {
     CoroutineScope(Dispatchers.Main).launch {

    }
}
