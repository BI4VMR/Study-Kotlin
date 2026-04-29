package net.bi4vmr.study.base

/**
 * 坐标类（使用泛型）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Location2<T1 : Any, T2 : Any>(
    x: T1,
    y: T2
) {

    // 变量的类型由调用者决定
    var x: T1 = x
        private set

    var y: T2 = y
        private set

    // 设置坐标
    fun setXY(x: T1, y: T2) {
        this.x = x
        this.y = y
    }
}
