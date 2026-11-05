package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice

/**
 * ADB 执行器与设备列表。
 *
 * 选中状态由 [GlobalStateManager] 管理，全局可复用。
 */
@Composable
fun DeviceSelector(
    globalStateManager: GlobalStateManager,
    modifier: Modifier = Modifier
) {
    val logicManager: LogicManager = globalStateManager.logicManager
    val executors: List<ADBExecutor> by logicManager.executorsFlow.collectAsStateWithLifecycle()
    val selectedExecutor: ADBExecutor by logicManager.selectedExecutorFlow.collectAsStateWithLifecycle()
    val devices: List<ADBDevice> by logicManager.devicesFlow.collectAsStateWithLifecycle()
    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        Text(
            text = "设备列表",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // 执行器列表
        SegmentedTabRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            executors.forEach { executor ->
                SegmentedTab(
                    selected = executor == selectedExecutor,
                    onClick = { logicManager.selectExecutor(executor) }
                ) {
                    Text(text = executor.getDisplayName(), maxLines = 1)
                }
            }
        }

        // 设备列表
        DeviceList(
            globalStateManager = globalStateManager,
            executor = selectedExecutor,
            devices = devices,
            selected = selectedDevice,
            onSelect = { logicManager.selectDevice(it) },
            modifier = Modifier.weight(1F)
        )
    }
}

@Composable
private fun DeviceList(
    globalStateManager: GlobalStateManager,
    executor: ADBExecutor,
    devices: List<ADBDevice>,
    selected: ADBDevice?,
    onSelect: (ADBDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val state: ServiceState by executor.getStateFlow().collectAsStateWithLifecycle()
    val error: Exception? by executor.getExceptionFlow().collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        when {
            state == ServiceState.PREPARING -> {
                ExecutorStatePanel(modifier = Modifier.weight(1F)) {
                    CircularProgressIndicator()
                    Text("正在连接 ${executor.getDisplayName()} ...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            state == ServiceState.INIT -> {
                ExecutorStatePanel(modifier = Modifier.weight(1F)) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (error != null) "初始化失败" else "未连接",
                        style = MaterialTheme.typography.titleSmall
                    )
                    error?.let {
                        Text(
                            text = it.message ?: it.javaClass.simpleName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(onClick = { executor.init() }) {
                        Icon(Icons.Filled.Refresh, null)
                        Text("重新初始化")
                    }
                }
            }

            devices.isEmpty() -> {
                ExecutorStatePanel(modifier = Modifier.weight(1F)) {
                    Text("暂无设备", style = MaterialTheme.typography.bodyMedium)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1F),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(devices, key = { it.getSN() }) { device ->
                        val isSelected = (device.getSN() == selected?.getSN())
                        DeviceListItem(
                            device = device,
                            selected = isSelected,
                            onClick = { onSelect(device) }
                        )
                    }
                }
            }
        }

        var connectDeviceDialogState: Boolean by remember { mutableStateOf(false) }
        var connectProxyDialogState: Boolean by remember { mutableStateOf(false) }

        Row {
            OutlinedButton(
                onClick = { connectDeviceDialogState = true }
            ) {
                Icon(Icons.Filled.Add, null)
                Text("添加设备")
            }

            OutlinedButton(
                onClick = { connectProxyDialogState = true }
            ) {
                Icon(Icons.Filled.Add, null)
                Text("添加转发器")
            }
        }

        if (connectDeviceDialogState) {
            DeviceConnectDialog(
                globalStateManager,
                onDismiss = { connectDeviceDialogState = false }
            )
        }

        if (connectProxyDialogState) {
            ProxyConnectDialog(
                globalStateManager,
                onDismiss = { connectProxyDialogState = false }
            )
        }
    }
}


/**
 * 执行器状态面板，用于显示加载中、错误等非正常状态。
 */
@Composable
private fun ExecutorStatePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun DeviceListItem(
    device: ADBDevice,
    selected: Boolean,
    onClick: (device: ADBDevice) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(device) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.Layout.Padding)
        ) {
            Icon(Icons.Filled.Devices, null)
            Spacer(Modifier.width(Dimens.Layout.Space))
            Text(device.getSN(), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1F))
            Text(device.getState().name, style = MaterialTheme.typography.labelSmall)
        }
    }
}
