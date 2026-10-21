package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.component.DeviceSidebar
import net.bi4vmr.androidtoolkit.ui.component.InfoRow
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo

/**
 * 显示管理页面。
 *
 * 三栏布局：设备侧边栏、显示屏列表、显示屏详情。显示屏相关状态仅保存在本组件内。
 */
@Composable
fun DisplayPage(globalStateManager: GlobalStateManager) {
    val selectedDevice: ADBDevice? by globalStateManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    var displays: List<DisplayInfo> by remember { mutableStateOf(emptyList()) }
    var selectedDisplayID: String? by remember { mutableStateOf(null) }
    var loading: Boolean by remember { mutableStateOf(false) }
    // 递增以触发重新加载
    var refreshTick: Int by remember { mutableStateOf(0) }

    // 设备变化或手动刷新时重新获取显示屏列表
    LaunchedEffect(selectedDevice?.getSN(), refreshTick) {
        val sn = selectedDevice?.getSN()
        if (sn == null) {
            displays = emptyList()
            selectedDisplayID = null
            return@LaunchedEffect
        }

        loading = true
        try {
            val result = globalStateManager.getDisplays(sn)
            displays = result
            // 保留仍然存在的选中项
            if (result.none { it.id == selectedDisplayID }) {
                selectedDisplayID = null
            }
        } finally {
            loading = false
        }
    }

    val selectedDisplay: DisplayInfo? = displays.firstOrNull { it.id == selectedDisplayID }

    Row(modifier = Modifier.fillMaxSize()) {
        DeviceSidebar(
            globalStateManager = globalStateManager,
            modifier = Modifier.width(280.dp).fillMaxHeight()
        )

        VerticalDivider()

        DisplayList(
            hasDevice = selectedDevice != null,
            displays = displays,
            selectedID = selectedDisplayID,
            loading = loading,
            onSelect = { selectedDisplayID = it.id },
            onRefresh = { refreshTick++ },
            modifier = Modifier.width(240.dp).fillMaxHeight()
        )

        VerticalDivider()

        DisplayDetail(
            display = selectedDisplay,
            modifier = Modifier.weight(1F).fillMaxHeight()
        )
    }
}

@Composable
private fun DisplayList(
    hasDevice: Boolean,
    displays: List<DisplayInfo>,
    selectedID: String?,
    loading: Boolean,
    onSelect: (DisplayInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "显示屏",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (!hasDevice) {
            Box(Modifier.weight(1F).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("请先选择设备")
            }
            return
        }

        Box(Modifier.weight(1F).fillMaxWidth()) {
            if (displays.isEmpty()) {
                Text(
                    text = if (loading) "加载中…" else "未获取到显示屏",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displays, key = { it.id }) { display ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(display) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (display.id == selectedID) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Display ${display.id}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${display.width} x ${display.height}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onRefresh,
            enabled = !loading,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("刷新")
        }
    }
}

@Composable
private fun DisplayDetail(display: DisplayInfo?, modifier: Modifier = Modifier) {
    if (display == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("请选择一个显示屏", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Display ${display.id}", style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        InfoRow("屏幕 ID", display.id)
        InfoRow("物理 ID", display.uniqueID.ifEmpty { "-" })
        InfoRow("物理 ID 索引", display.getUniqueIndex().ifEmpty { "-" })
        InfoRow("分辨率", "${display.width} x ${display.height}")
        InfoRow("所属设备", display.sn.ifEmpty { "-" })
    }
}
