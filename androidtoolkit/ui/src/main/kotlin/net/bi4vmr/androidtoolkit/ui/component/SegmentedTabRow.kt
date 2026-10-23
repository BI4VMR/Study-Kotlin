package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Segment 样式的通用选项卡容器，用法类似 [androidx.compose.material3.SingleChoiceSegmentedButtonRow]。
 *
 * 选项卡的数量与内容由调用者通过 [SegmentedTab] 自由决定。
 * 选项卡总宽度超出容器时，可通过鼠标滚轮或按住拖拽进行横向滑动。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SegmentedTabRow(
    modifier: Modifier = Modifier,
    content: @Composable SegmentedTabRowScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(8.dp)
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .horizontalScroll(scrollState)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.sumOf { it.scrollDelta.let { d -> (d.x + d.y).toDouble() } }.toFloat()
                scope.launch { scrollState.scrollBy(delta * 40F) }
                event.changes.forEach { it.consume() }
            }
    ) {
        SegmentedTabRowScopeImpl(this).content()
    }
}

/** [SegmentedTabRow] 的作用域。 */
interface SegmentedTabRowScope : RowScope

private class SegmentedTabRowScopeImpl(rowScope: RowScope) : SegmentedTabRowScope, RowScope by rowScope

/**
 * 单个选项卡，标签内容由 [label] 自定义。
 */
@Composable
fun SegmentedTabRowScope.SegmentedTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            label()
        }
    }
}
