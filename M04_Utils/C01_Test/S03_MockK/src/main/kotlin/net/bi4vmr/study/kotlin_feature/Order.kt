package net.bi4vmr.study.kotlin_feature

/**
 * 订单类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class Order {

    private val list: MutableList<String> = mutableListOf()

    constructor(goods: String) {
        list.add(goods)
    }

    fun showInfo1(): String = list.toString()

    fun showInfo2(): String = list.toString()
}
