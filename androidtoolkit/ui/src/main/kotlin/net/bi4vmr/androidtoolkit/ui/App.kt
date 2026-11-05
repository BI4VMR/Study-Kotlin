package net.bi4vmr.androidtoolkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.bi4vmr.androidtoolkit.ui.apps.AppsPage
import net.bi4vmr.androidtoolkit.ui.common.Pages
import net.bi4vmr.androidtoolkit.ui.component.ToastHost
import net.bi4vmr.androidtoolkit.ui.display.ADBCastWindow
import net.bi4vmr.androidtoolkit.ui.display.CastRecord
import net.bi4vmr.androidtoolkit.ui.display.DisplayPage
import net.bi4vmr.androidtoolkit.ui.files.FilesPage
import net.bi4vmr.androidtoolkit.ui.generic.GenericPage
import net.bi4vmr.androidtoolkit.ui.setting.SettingPage

/**
 * 应用主界面。
 */
@Composable
fun App() {
    val globalStateManager: GlobalStateManager = remember { GlobalStateManager() }
    var currentPage: Pages by remember { mutableStateOf(Pages.GENERAL) }

    Surface {
        /* 根容器 */
        Row {
            /* 左栏：导航栏 */
            NavigationRail {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp, 16.dp)
                ) {
                    Pages.entries.forEach { page ->
                        NavigationRailItem(
                            selected = (currentPage == page),
                            icon = { Icon(page.icon, null) },
                            label = { Text(page.titleRes) },
                            onClick = { currentPage = page }
                        )
                    }
                }
            }

            VerticalDivider()

            /* 右栏：选中的页面 */
            when (currentPage) {
                Pages.GENERAL -> GenericPage(globalStateManager)
                Pages.DISPLAY -> DisplayPage(globalStateManager)
                Pages.APP -> AppsPage(globalStateManager)
                Pages.FILE -> FilesPage(globalStateManager)
                Pages.SETTING -> SettingPage(globalStateManager)
            }
        }

        /* 全局消息气泡层 */
        ToastHost(globalStateManager.toastManager)
    }


    /* 配对申请确认：多个申请按顺序逐个展示 */
    val pendingPairings by globalStateManager.logicManager.pendingPairingsFlow.collectAsState()
    val pending = pendingPairings.firstOrNull()
    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { p.reject() },
            title = { Text("配对申请") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("设备名称：${p.getClientName()}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "客户端证书指纹：${p.getClientFingerprint()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("配对码：${p.getSASCode()}", style = MaterialTheme.typography.headlineMedium)
                    Text("请核对客户端显示的配对码，仅在两端完全一致时批准；不一致说明通信可能被劫持。")
                }
            },
            confirmButton = { TextButton(onClick = { p.approve() }) { Text("批准") } },
            dismissButton = { TextButton(onClick = { p.reject() }) { Text("拒绝") } }
        )
    }


    /* 全局窗口管理 */
    globalStateManager.windowManager.windows.forEach { record ->
        key(record.id) {
            if (record is CastRecord) {
                ADBCastWindow(globalStateManager, record)
            }
        }
    }
}
