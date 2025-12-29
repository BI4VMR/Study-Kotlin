package net.bi4vmr.study.advance

/**
 * 内存相关工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class MemoryInfo {

    private val runtime = Runtime.getRuntime()

    fun getTotalMemory(): Long = runtime.totalMemory()

    fun getFreeMemory(): Long = runtime.freeMemory()
}
