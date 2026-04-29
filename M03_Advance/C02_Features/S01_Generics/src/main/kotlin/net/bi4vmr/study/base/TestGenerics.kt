package net.bi4vmr.study.base

import java.math.BigDecimal
import kotlin.reflect.full.memberProperties

/**
 * 示例代码：泛型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    example04()
}


/**
 * 示例一：表示二维坐标。
 *
 * 在本示例中，我们定义一个类表示二维坐标，允许调用者传入整数、小数等多种形式的原始数据。
 */
fun example01() {
    // 创建实例并设置初始坐标
    val location = Location1()
    location.setXY(100, 25.1082)

    // 读取数据时需要判断具体的类型
    when (val x = location.x) {
        is Int -> {
            // 将数据转换为具体类型再操作
            val resultX = x.compareTo(100) == 0
            println("x = 100? $resultX")
        }
        is Double -> {
            val resultX = x.compareTo(100.0) == 0
            println("x = 100.0? $resultX")
        }
        else -> {
            throw IllegalArgumentException("参数类型错误！");
        }
    }

    // 比较Y轴的数据时也需要判断类型，此处省略相关代码。


    // 随便传入一些非法数据也不会报错，直到读取数据时才能发现错误。
    location.setXY("这是一些文本", BigDecimal("0.01"))
}


/**
 * 示例二：表示二维坐标（基于泛型）。
 *
 * 在本示例中，我们定义一个类表示二维坐标，允许调用者传入整数、小数等多种形式的原始数据。
 */
fun example02() {
    // 创建实例并设置初始坐标
    val location = Location2(100, 25.1082)

    // 读取数据时无需判断类型，与创建实例时的类型一致。
    val x: Int = location.x
    val resultX = x.compareTo(100) == 0
    println("x = 100? $resultX")

    // 比较Y轴的数据时无需判断类型，此处省略相关代码。


    // 参数类型与声明实例时的泛型类型不一致，编译阶段将会报错。
    // location.setXY("这是一些文本", BigDecimal("0.01"))
}


/**
 * 示例三：泛型方法。
 *
 * 在本示例中，我们定义一个泛型方法，将数组转换为对应类型的列表。
 */
fun example03() {
    // 待转换的数组
    val array = arrayOf(1, 2, 3, 4, 5)

    // 调用泛型方法，类型由参数或返回值推断得出，此处为Integer。
    val list: List<Int> = arrayToList(array)

    // 我们也可以显式指定类型参数
    // arrayToList<Int>(array)

    println(list)
}

/**
 * 将数组转换为列表。
 *
 * 该方法提供了一种将泛型数组转换为泛型列表的便捷方式。它避免了直接操作数组可能带来的局限性，比如数组长度不可变。
 * 使用列表可以使元素的添加、删除等操作更加灵活。
 *
 * @param[F]     泛型类型，表示数组和列表中元素的类型。
 * @param[array] 输入的泛型数组，将被转换为列表。
 * @return 返回一个新的列表，包含与输入数组相同的元素。
 */
fun <F> arrayToList(array: Array<F>): MutableList<F> {
    val list = mutableListOf<F>()
    for (element in array) {
        list.add(element)
    }
    return list
}


/**
 * 示例四：类型擦除。
 * <p>
 * 在本示例中，我们通过反射获取泛型变量的类型，观察类型擦除的效果。
 */
fun example04() {
    // 创建实例并设置初始坐标
    val location = Location2(100, 25.1082)

    // 尝试获取变量的类型
    location::class.memberProperties
        .forEach {
            println("变量名称：[${it.name}] 变量类型：[${it.returnType}]")
        }
}
