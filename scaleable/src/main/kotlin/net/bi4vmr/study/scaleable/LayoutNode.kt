package net.bi4vmr.study.scaleable

import java.util.UUID

/**
 * 分栏方向。
 *
 * [Horizontal] 表示左右分栏（Row），[Vertical] 表示上下分栏（Column）。
 */
enum class Orientation { Horizontal, Vertical }

/**
 * 布局树节点。
 */
sealed interface LayoutNode {
    val id: String

    /** 叶子节点：实际显示内容的面板。 */
    data class Leaf(
        override val id: String = UUID.randomUUID().toString(),
        val title: String = "Panel"
    ) : LayoutNode

    /** 容器节点：按方向与权重分配空间给子节点。 */
    data class Split(
        override val id: String = UUID.randomUUID().toString(),
        val orientation: Orientation,
        val children: List<LayoutNode>,
        val weights: List<Float>
    ) : LayoutNode
}

/**
 * 将指定面板一分为二，新面板位于其后方（右侧或下方）。
 *
 * 若父容器方向与分割方向一致，则直接插入兄弟节点，避免产生多余嵌套。
 */
fun LayoutNode.split(targetId: String, orientation: Orientation, newLeaf: LayoutNode.Leaf): LayoutNode =
    when (this) {
        is LayoutNode.Leaf ->
            if (id == targetId) {
                LayoutNode.Split(
                    orientation = orientation,
                    children = listOf(this, newLeaf),
                    weights = listOf(0.5F, 0.5F)
                )
            } else {
                this
            }

        is LayoutNode.Split -> {
            val index = children.indexOfFirst { it is LayoutNode.Leaf && it.id == targetId }
            if (index >= 0 && this.orientation == orientation) {
                // 同方向：平分目标面板的权重，并插入新面板
                val half = weights[index] / 2
                copy(
                    children = children.toMutableList().apply { add(index + 1, newLeaf) },
                    weights = weights.toMutableList().apply {
                        this[index] = half
                        add(index + 1, half)
                    }
                )
            } else {
                copy(children = children.map { it.split(targetId, orientation, newLeaf) })
            }
        }
    }

/**
 * 移除指定面板，仅剩一个子节点的容器会被折叠。返回 `null` 表示整棵树已被清空。
 */
fun LayoutNode.remove(targetId: String): LayoutNode? =
    when (this) {
        is LayoutNode.Leaf -> if (id == targetId) null else this
        is LayoutNode.Split -> {
            val kept = children.zip(weights)
                .mapNotNull { (child, weight) -> child.remove(targetId)?.let { it to weight } }
            when (kept.size) {
                0 -> null
                1 -> kept.first().first
                else -> {
                    val sum = kept.sumOf { it.second.toDouble() }.toFloat()
                    copy(children = kept.map { it.first }, weights = kept.map { it.second / sum })
                }
            }
        }
    }

/**
 * 调整容器中第 [index] 与第 [index] + 1 个子节点之间的分隔位置。
 *
 * @param delta 权重变化量，正数表示分隔条向右（下）移动。
 */
fun LayoutNode.resize(splitId: String, index: Int, delta: Float, minWeight: Float = 0.05F): LayoutNode =
    when (this) {
        is LayoutNode.Leaf -> this
        is LayoutNode.Split ->
            if (id == splitId) {
                val a = weights[index]
                val b = weights[index + 1]
                val d = delta.coerceIn(minWeight - a, b - minWeight)
                copy(weights = weights.toMutableList().apply {
                    this[index] = a + d
                    this[index + 1] = b - d
                })
            } else {
                copy(children = children.map { it.resize(splitId, index, delta, minWeight) })
            }
    }
