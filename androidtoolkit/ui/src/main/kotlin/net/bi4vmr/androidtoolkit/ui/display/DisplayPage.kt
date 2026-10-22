package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.WindowManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.component.DeviceSelector
import net.bi4vmr.androidtoolkit.ui.component.InfoRow
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo

/**
 * 显示管理页面。
 */
@Composable
fun DisplayPage(globalStateManager: GlobalStateManager) {
    val logicManager: LogicManager = globalStateManager.logicManager
    val viewModel: DisplayPageViewModel = viewModel { DisplayPageViewModel(logicManager) }

    val displays: List<DisplayInfo> by viewModel.displaysFlow.collectAsStateWithLifecycle()
    val selectedDisplay: DisplayInfo? by viewModel.selectedDisplayFlow.collectAsStateWithLifecycle()


    // 根容器
    Row(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.width(280.dp)) {
            DeviceSelector(
                globalStateManager = globalStateManager,
                modifier = Modifier.width(280.dp).weight(0.5F)
            )

            HorizontalDivider()

            DisplayList(
                displays = displays,
                selectedDisplay = selectedDisplay,
                onSelect = { viewModel.selectDisplay(it) },
                onRefresh = { viewModel.refreshDisplays() },
                modifier = Modifier.width(280.dp).weight(0.5F)
            )
        }

        VerticalDivider()

        DisplayDetail(
            display = selectedDisplay,
            onCast = { globalStateManager.windowManager.addWindow(WindowManager.Record()) },
            modifier = Modifier.weight(1F)
        )
    }
}


@Composable
private fun DisplayList(
    displays: List<DisplayInfo>,
    selectedDisplay: DisplayInfo?,
    onSelect: (DisplayInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row {
            Text(text = "显示屏")

            Spacer(Modifier.weight(1F))

            OutlinedButton(
                onClick = onRefresh
            ) {
                Text("刷新")
            }
        }


        if (displays.isEmpty()) {
            Box(Modifier.weight(1F).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("请先选择设备")
            }
            return@Column
        }

        Box(Modifier.weight(1F).fillMaxWidth()) {
            LazyColumn(
                contentPadding = PaddingValues(Dimens.Layout.Padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space)
            ) {
                items(displays, key = { it.id }) { display ->
                    DisplayListItem(
                        display = display,
                        selectedDisplay = selectedDisplay,
                        onClick = onSelect
                    )
                }
            }
        }
    }
}


@Composable
private fun DisplayListItem(
    display: DisplayInfo,
    selectedDisplay: DisplayInfo?,
    onClick: (display: DisplayInfo) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (display == selectedDisplay) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(display) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.Layout.Padding)
        ) {
            Icon(Icons.Default.Cast, null)
            Spacer(Modifier.width(Dimens.Layout.Space))
            Text(display.id)
            Spacer(Modifier.weight(1F))
            Text("${display.width} x ${display.height}")
        }
    }
}

@Composable
private fun DisplayDetail(display: DisplayInfo?, onCast: () -> Unit, modifier: Modifier = Modifier) {
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
        InfoRow("分辨率", "${display.width} x ${display.height}")
        InfoRow("所属设备", display.sn.ifEmpty { "-" })

        Button(onClick = onCast) {
            Text("投屏")
        }
    }
}
