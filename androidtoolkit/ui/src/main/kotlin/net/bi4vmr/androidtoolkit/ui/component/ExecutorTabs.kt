package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.ADBExecutor

/**
 * Segment 样式的执行器选项卡。
 *
 * 选项卡总宽度超出容器时，可通过鼠标滚轮或按住拖拽进行横向滑动。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExecutorTabs(
    executors: List<ADBExecutor>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(8.dp)
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.sumOf { it.scrollDelta.let { d -> (d.x + d.y).toDouble() } }.toFloat()
                scope.launch { scrollState.scrollBy(delta * 40F) }
                event.changes.forEach { it.consume() }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    scope.launch { scrollState.scrollBy(-dragAmount.x) }
                }
            }
            .horizontalScroll(scrollState)
    ) {
        executors.forEachIndexed { index, executor ->
            val selected = index == selectedIndex
            val label =executor.getDisplayName()
            Box(
                modifier = Modifier
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
