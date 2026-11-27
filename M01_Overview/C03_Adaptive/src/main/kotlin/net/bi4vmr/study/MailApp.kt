package net.bi4vmr.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch

/**
 * 列表-详情自适应布局示例。
 *
 * 布局会根据当前窗口宽度自动调整：
 * - 宽屏（Expanded 及以上）：列表与详情左右并排显示。
 * - 窄屏（Compact、Medium）：只显示一个面板，点击列表项后切换到详情。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MailApp() {
    // 导航器负责列表与详情之间的跳转，以及返回栈的管理。
    // 泛型参数表示 contentKey 的类型，这里的 contentKey 是邮件ID。
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()
    val scope = rememberCoroutineScope()

    // 获取当前窗口的尺寸等级，窗口缩放时会自动更新。
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

    ListDetailPaneScaffold(
        // 布局指令：根据窗口尺寸计算每个面板的最大数量、边距等参数。
        directive = navigator.scaffoldDirective,
        // 当前布局状态：表示列表、详情等面板应该显示还是隐藏。
        value = navigator.scaffoldValue,

        // 列表面板：窄屏时是主界面，宽屏时显示在左侧。
        listPane = {
            // AnimatedPane 为面板提供默认的进入、退出动画，建议作为面板的根布局。
            AnimatedPane {
                Column(Modifier.fillMaxSize()) {
                    MailListHeader(windowSizeClass)
                    HorizontalDivider()

                    LazyColumn(Modifier.weight(1f)) {
                        items(sampleMails, key = { it.id }) { mail ->
                            ListItem(
                                headlineContent = { Text(mail.subject) },
                                supportingContent = { Text(mail.sender) },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        // 跳转到详情面板，并附带当前邮件的ID。
                                        navigator.navigateTo(
                                            pane = ListDetailPaneScaffoldRole.Detail,
                                            contentKey = mail.id,
                                        )
                                    }
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },

        // 详情面板：窄屏时点击列表项后进入，宽屏时显示在右侧。
        detailPane = {
            AnimatedPane {
                // 从导航器读取当前详情面板对应的 contentKey（邮件ID）。
                val selectedID = navigator.currentDestination?.contentKey
                val selectedMail = sampleMails.firstOrNull { it.id == selectedID }

                if (selectedMail == null) {
                    EmptyDetailPane()
                } else {
                    MailDetail(selectedMail) {
                        scope.launch {
                            // 宽屏时列表和详情同时可见，
                            // 使用 PopUntilContentChange 才能通过“返回”清空详情。
                            navigator.navigateBack(
                                BackNavigationBehavior.PopUntilContentChange
                            )
                        }
                    }
                }
            }
        },
    )
}

/** 列表面板顶部的标题和窗口宽度断点信息。 */
@Composable
private fun MailListHeader(windowSizeClass: WindowSizeClass) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "收件箱",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "窗口宽度断点：${windowSizeClass.widthBreakpointName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 详情面板中尚未选择邮件时的占位内容。 */
@Composable
private fun EmptyDetailPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "请从左侧选择一封邮件",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 邮件详情内容。 */
@Composable
private fun MailDetail(mail: Mail, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = mail.subject,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "发件人：${mail.sender}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = mail.content,
            style = MaterialTheme.typography.bodyLarge,
        )

        // 将“返回”按钮推到面板底部。
        Spacer(Modifier.weight(1f))
        Button(onClick = onBack) {
            Text("返回列表")
        }
    }
}

/** 将窗口宽度断点转换为便于阅读的文本。 */
private fun WindowSizeClass.widthBreakpointName(): String = when {
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) ->
        "ExtraLarge（≥1600dp）"

    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) ->
        "Large（≥1200dp）"

    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
        "Expanded（≥840dp）"

    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
        "Medium（≥600dp）"

    else -> "Compact（<600dp）"
}
