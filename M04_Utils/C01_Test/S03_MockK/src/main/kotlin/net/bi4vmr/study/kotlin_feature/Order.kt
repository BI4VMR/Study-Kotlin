package net.bi4vmr.study.kotlin_feature

/**
 * 订单类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Order(
    private val goods: String
) {

    fun showInfo1(): String = goods

    fun showInfo2(): String = goods
}
