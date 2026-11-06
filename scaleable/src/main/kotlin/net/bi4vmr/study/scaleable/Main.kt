package net.bi4vmr.study.scaleable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "自定义分栏布局示例"
    ) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                var counter by remember { mutableIntStateOf(1) }
                var root by remember { mutableStateOf<LayoutNode?>(LayoutNode.Leaf(title = "Panel 1")) }

                val current = root
                if (current == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { root = LayoutNode.Leaf(title = "Panel ${++counter}") }) {
                            Text("新建面板")
                        }
                    }
                } else {
                    LayoutRenderer(current, onAction = { action ->
                        // 基于最新状态计算，避免连续拖拽事件读取到旧树
                        val latest = root ?: return@LayoutRenderer
                        root = when (action) {
                            is LayoutAction.Split ->
                                latest.split(action.targetId, action.orientation, LayoutNode.Leaf(title = "Panel ${++counter}"))

                            is LayoutAction.Remove -> latest.remove(action.targetId)
                            is LayoutAction.Resize -> latest.resize(action.splitId, action.index, action.delta)
                        }
                    })
                }
            }
        }
    }
}
