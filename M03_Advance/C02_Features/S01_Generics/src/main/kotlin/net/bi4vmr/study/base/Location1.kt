package net.bi4vmr.study.base

/**
 * 坐标类（未使用泛型）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Location1 {

    var x: Any = 0
        private set

    var y: Any = 0
        private set

    // 设置坐标
    fun setXY(x: Any, y: Any) {
        this.x = x
        this.y = y
    }
}
