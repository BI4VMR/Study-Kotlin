package net.bi4vmr.study.scaleable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation as GestureOrientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.Cursor

/**
 * 布局操作，由渲染层抛出，在状态持有者处处理。
 */
sealed interface LayoutAction {
    data class Split(val targetId: String, val orientation: Orientation) : LayoutAction
    data class Remove(val targetId: String) : LayoutAction
    data class Resize(val splitId: String, val index: Int, val delta: Float) : LayoutAction
}

/**
 * 递归渲染布局树。
 */
@Composable
fun LayoutRenderer(
    node: LayoutNode,
    onAction: (LayoutAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (node) {
        is LayoutNode.Leaf -> PanelView(node, onAction, modifier)
        is LayoutNode.Split -> SplitView(node, onAction, modifier)
    }
}

@Composable
private fun SplitView(
    node: LayoutNode.Split,
    onAction: (LayoutAction) -> Unit,
    modifier: Modifier
) {
    // 获取容器尺寸，用于将拖拽像素换算为权重
    BoxWithConstraints(modifier.fillMaxSize()) {
        val horizontal = node.orientation == Orientation.Horizontal
        val totalPx = with(LocalDensity.current) {
            (if (horizontal) maxWidth else maxHeight).toPx()
        }

        val children: @Composable (Modifier.(Float) -> Modifier) -> Unit = { weightOf ->
            node.children.forEachIndexed { i, child ->
                key(child.id) {
                    LayoutRenderer(child, onAction, Modifier.weightOf(node.weights[i]))
                }
                if (i < node.children.lastIndex) {
                    Divider(horizontal) { deltaPx ->
                        onAction(LayoutAction.Resize(node.id, i, deltaPx / totalPx))
                    }
                }
            }
        }

        if (horizontal) {
            Row(Modifier.fillMaxSize()) { children { weight(it) } }
        } else {
            Column(Modifier.fillMaxSize()) { children { weight(it) } }
        }
    }
}

@Composable
private fun Divider(horizontal: Boolean, onDrag: (Float) -> Unit) {
    val state = rememberDraggableState(onDrag)
    val cursor = if (horizontal) Cursor.E_RESIZE_CURSOR else Cursor.N_RESIZE_CURSOR
    Box(
        Modifier
            .then(if (horizontal) Modifier.width(6.dp).fillMaxHeight() else Modifier.height(6.dp).fillMaxWidth())
            .background(MaterialTheme.colorScheme.outlineVariant)
            .pointerHoverIcon(PointerIcon(Cursor(cursor)))
            .draggable(state, if (horizontal) GestureOrientation.Horizontal else GestureOrientation.Vertical)
    )
}

@Composable
private fun PanelView(
    node: LayoutNode.Leaf,
    onAction: (LayoutAction) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(node.title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAction(LayoutAction.Split(node.id, Orientation.Horizontal)) }) {
                    Text("左右分栏")
                }
                OutlinedButton(onClick = { onAction(LayoutAction.Split(node.id, Orientation.Vertical)) }) {
                    Text("上下分栏")
                }
                OutlinedButton(onClick = { onAction(LayoutAction.Remove(node.id)) }) {
                    Text("关闭")
                }
            }
        }
    }
}
