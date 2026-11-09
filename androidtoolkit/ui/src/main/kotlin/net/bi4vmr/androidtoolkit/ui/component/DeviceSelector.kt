package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.placeholder_device_name
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import org.jetbrains.compose.resources.getString

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

    var connectProxyDialogState: Boolean by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
        modifier = modifier.padding(Dimens.Layout.Padding)
    ) {
        Text(
            text = "设备管理",
            style = MaterialTheme.typography.titleSmall
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 执行器列表
            SegmentedTabRow {
                executors.forEach { executor ->
                    SegmentedTab(
                        selected = executor == selectedExecutor,
                        onClick = { logicManager.selectExecutor(executor) }
                    ) {
                        Text(text = executor.getDisplayName(), maxLines = 1)

                        if (executor is RemoteExecutor) {
                            Spacer(Modifier.width(Dimens.Layout.SpaceSmall))
                            IconButton(
                                onClick = { logicManager.removeRemoteExecutor(executor.id) },
                                modifier = Modifier.size(Dimens.Icon.Small)
                            ) {
                                Icon(Icons.Filled.Close, null)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1F))

            Spacer(Modifier.width(Dimens.Layout.SpaceSmall))

            // 按钮：连接远程执行器
            IconButton(
                onClick = { connectProxyDialogState = true },
                modifier = Modifier.size(Dimens.Icon.Normal)
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(Dimens.Icon.Normal))
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

        if (connectProxyDialogState) {
            ProxyConnectDialog(
                globalStateManager,
                onDismiss = { connectProxyDialogState = false }
            )
        }
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

    var connectDeviceDialogState: Boolean by remember { mutableStateOf(false) }


    LaunchedEffect(executor) {
        if (executor.getStateFlow().value == ServiceState.INIT) {
            executor.init()
        }
    }

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
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(devices, key = { it.getSN() }) { device ->
                        val isSelected = (device.getSN() == selected?.getSN())
                        DeviceListItem(
                            executor = executor,
                            device = device,
                            selected = isSelected,
                            onClick = { onSelect(device) }
                        )
                    }
                }
            }
        }

        Row {
            OutlinedButton(
                onClick = { connectDeviceDialogState = true }
            ) {
                Icon(Icons.Filled.Add, null)
                Text("添加设备")
            }
        }

        if (connectDeviceDialogState) {
            DeviceConnectDialog(
                globalStateManager,
                onDismiss = { connectDeviceDialogState = false }
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
    executor: ADBExecutor,
    device: ADBDevice,
    selected: Boolean,
    onClick: (device: ADBDevice) -> Unit
) {
    val sn = device.getSN()
    val state = device.getState()

    val name: String by produceState(initialValue = "-", executor, sn, state) {
        value = executor.getName(sn, getString(Res.string.placeholder_device_name))
    }
    val type: DeviceType? by produceState(initialValue = null, executor, sn, state) {
        value = executor.getDeviceType(sn)
    }

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
        onClick = { onClick(device) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.Layout.ListItemPadding)
        ) {
            Icon(Icons.Filled.Devices, null)
            Spacer(Modifier.width(Dimens.Layout.Space))
            Column {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(sn, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Spacer(Modifier.weight(1F))

            // 按钮：连接远程执行器
            if (type == DeviceType.NETWORK) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(Dimens.Icon.Small)
                ) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(Dimens.Icon.Small))
                }
            }
        }
    }
}
