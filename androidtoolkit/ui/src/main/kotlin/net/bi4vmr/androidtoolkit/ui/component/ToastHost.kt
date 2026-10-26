package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 消息类型。
 */
enum class ToastType { INFO, SUCCESS, ERROR }

/**
 * 消息数据。
 */
data class ToastMessage(
    val id: Long,
    val durationMs: Long,
    val content: @Composable () -> Unit
)

/**
 * 默认样式的消息气泡。
 */
@Composable
fun DefaultToastContent(text: String, type: ToastType = ToastType.INFO) {
    val color = when (type) {
        ToastType.INFO -> MaterialTheme.colorScheme.secondaryContainer
        ToastType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        ToastType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color,
        shadowElevation = 4.dp
    ) {
        Text(text, Modifier.padding(12.dp))
    }
}

/**
 * 全局消息管理器。
 *
 * 新消息插入队列头部，显示在最上方。
 */
class ToastManager {

    private var nextId = 0L
    val messages = mutableStateListOf<ToastMessage>()

    /**
     * 显示默认样式的文本消息。
     */
    fun show(text: String, type: ToastType = ToastType.INFO, durationMs: Long = 3000L) {
        show(durationMs) { DefaultToastContent(text, type) }
    }

    /**
     * 显示由调用者自定义内容的消息。
     */
    fun show(durationMs: Long = 3000L, content: @Composable () -> Unit) {
        messages.add(0, ToastMessage(nextId++, durationMs, content))
    }

    fun dismiss(id: Long) {
        messages.removeAll { it.id == id }
    }
}

/**
 * 全局消息气泡层，叠加在页面右上角。
 *
 * 仅消息区域占用点击事件，其余区域事件透传给下层界面。
 */
@Composable
fun ToastHost(
    manager: ToastManager,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd
) {
    val (isBottom, isStart) = alignment.edges()

    Box(modifier.fillMaxSize()) {
        // 无消息时不组合列表，避免空容器占位并遮挡下层控件
        if (manager.messages.isNotEmpty()) LazyColumn(
            modifier = Modifier
                .align(alignment)
                .padding(8.dp)
                .widthIn(max = 336.dp)
                .heightIn(max = 480.dp)
                .background(Color.Cyan),
            userScrollEnabled = false,
            // 预留空白，避免阴影被 LazyColumn 裁切
            contentPadding = PaddingValues(8.dp),
            horizontalAlignment = if (isStart) Alignment.Start else Alignment.End,
            // 靠下对齐时反向布局，最新消息仍位于最靠近屏幕边缘的外侧
            reverseLayout = isBottom,
            verticalArrangement = Arrangement.spacedBy(8.dp, if (isBottom) Alignment.Bottom else Alignment.Top)
        ) {
            items(manager.messages, key = { it.id }) { msg ->
                ToastItem(msg, isStart, Modifier.animateItem(), onTimeout = { manager.dismiss(msg.id) })
            }
        }
    }
}

@Composable
private fun ToastItem(msg: ToastMessage, fromStart: Boolean, modifier: Modifier, onTimeout: () -> Unit) {
    LaunchedEffect(msg.id) {
        delay(msg.durationMs)
        onTimeout()
    }

    // 仅新建时播放入场动画
    val visibleState = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visibleState.value = true }

    androidx.compose.animation.AnimatedVisibility(
        visible = visibleState.value,
        modifier = modifier,
        enter = slideInHorizontally(tween(250)) { if (fromStart) -it else it } + fadeIn(tween(250))
    ) {
        msg.content()
    }
}

/**
 * 解析对齐方式：返回（是否靠下，是否靠左）。居中视为靠上、从右侧进入。
 */
private fun Alignment.edges(): Pair<Boolean, Boolean> = when (this) {
    Alignment.TopStart, Alignment.CenterStart -> false to true
    Alignment.BottomStart -> true to true
    Alignment.BottomEnd, Alignment.BottomCenter -> true to false
    else -> false to false
}
