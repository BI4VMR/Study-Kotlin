package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.core.model.DummyDevice
import net.bi4vmr.androidtoolkit.core.model.LocalExecutor
import net.bi4vmr.androidtoolkit.core.model.RemoteExecutor
import net.bi4vmr.androidtoolkit.ui.*
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ServiceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBOutputParser
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * ADB 执行器与设备面板。
 *
 * 选中状态由 [GlobalStateManager] 管理，全局可复用。
 */
@Composable
fun DeviceSelectPanel(
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
            text = stringResource(Res.string.deviceselectpanel_title),
            style = MaterialTheme.typography.titleSmall
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1F)) {
                // 执行器列表
                SegmentedTabRow {
                    executors.forEach { executor ->
                        val name: String by produceState(initialValue = "-", executor) {
                            value = executor.getDisplayName()
                        }

                        SegmentedTab(
                            selected = executor == selectedExecutor,
                            onClick = { logicManager.selectExecutor(executor) }
                        ) {
                            Text(text = name, maxLines = 1)

                            if (executor is RemoteExecutor) {
                                Spacer(Modifier.width(Dimens.Layout.SpaceSmall))
                                IconButton(
                                    onClick = { logicManager.removeRemoteExecutor(executor.id) },
                                    modifier = Modifier.size(Dimens.Icon.Small)
                                ) {
                                    Icon(Icons.Close, null)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(Dimens.Layout.SpaceSmall))

            // 按钮：连接远程执行器
            IconButton(
                modifier = Modifier.size(Dimens.Icon.Normal),
                onClick = { connectProxyDialogState = true }
            ) {
                Icon(Icons.Add, null, Modifier.size(Dimens.Icon.Normal))
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


// 设备列表
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
                    Text("正在连接 ...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            state == ServiceState.INIT -> {
                ExecutorStatePanel(modifier = Modifier.weight(1F)) {
                    Icon(
                        imageVector = Icons.Close,
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
                    XOutlinedButton(onClick = { executor.init() }) {
                        Icon(Icons.Refresh, null)
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
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1F)
                ) {
                    items(devices, key = { it.getSN() }) { device ->
                        val isSelected = (device.getSN() == selected?.getSN())
                        DeviceListItem(
                            executor = executor,
                            device = device,
                            selected = isSelected,
                            onClick = {
                                if (device is DummyDevice) {
                                    globalStateManager.logicManager.connectRemoteDevice(device.host, device.port)
                                } else {
                                    onSelect(device)
                                }
                            },
                            onRemove = {
                                val host = ADBOutputParser.getRemoteDeviceHost(it.getSN())
                                val port = ADBOutputParser.getRemoteDevicePort(it.getSN())
                                globalStateManager.logicManager.removeRemoteDevice(host, port)
                            }
                        )
                    }
                }

                XOutlinedButton(
                    onClick = { connectDeviceDialogState = true }
                ) {
                    Icon(Icons.Add, null, Modifier.size(Dimens.Icon.Small))
                    Text("添加设备")
                }
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


// 设备列表项
@Composable
private fun DeviceListItem(
    executor: ADBExecutor,
    device: ADBDevice,
    selected: Boolean,
    onClick: (device: ADBDevice) -> Unit,
    onRemove: (device: ADBDevice) -> Unit
) {
    val sn = device.getSN()
    val state = device.getState()

    val name: String by produceState(
        initialValue = stringResource(Res.string.deviceselectpanel_unknown),
        executor,
        sn,
        state
    ) {
        val fallback = getString(Res.string.deviceselectpanel_unknown)
        value = if (device.isOnline()) {
            executor.getDeviceName(sn, fallback)
        } else {
            fallback
        }
    }

    val type: ConnectionType? by produceState(initialValue = null, executor, sn, state) {
        value = if (device is DummyDevice) {
            ConnectionType.NETWORK
        } else {
            executor.getDeviceType(sn)
        }
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
            Box {
                // 伪设备需显示加载中动画或特殊图标
                if (device is DummyDevice) {
                    if (device.connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(Dimens.Icon.Small))
                    } else {
                        Icon(
                            Icons.LinkOff,
                            null,
                            modifier = Modifier.size(Dimens.Icon.Small)
                        )
                    }
                    return@Box
                }

                // 设备图标
                Icon(
                    Icons.Phone,
                    null,
                    modifier = Modifier.size(Dimens.Icon.Small)
                )
            }

            Spacer(Modifier.width(Dimens.Layout.ListItemPadding))

            Column(modifier = Modifier.weight(1F)) {
                // 设备名称
                val nameText = if (device is DummyDevice) {
                    if (device.connecting) stringResource(Res.string.state_connecting)
                    else stringResource(Res.string.deviceselectpanel_remote)
                } else {
                    name
                }

                Text(
                    text = nameText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 状态指示器
                    StatusIndicator(state)

                    Spacer(Modifier.width(Dimens.Layout.SpaceSmall))

                    // 序列号
                    Text(
                        text = sn,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 按钮：删除远程设备（仅本地 ADB 的远程设备支持）
            if (executor is LocalExecutor && type == ConnectionType.NETWORK) {
                IconButton(
                    modifier = Modifier.size(Dimens.Icon.Small),
                    onClick = { onRemove(device) }
                ) {
                    Icon(Icons.Delete, null, Modifier.size(Dimens.Icon.Small))
                }
            }
        }
    }
}


// 设备状态指示器
@Composable
fun StatusIndicator(
    deviceState: DeviceState,
    modifier: Modifier = Modifier
) {
    val color: Color = when (deviceState) {
        DeviceState.ONLINE -> Color(0xFF4CAF50)
        DeviceState.UNAUTHORIZED -> Color(0xFFFFC107)
        DeviceState.BOOTLOADER -> Color(0xFF2196F3)
        DeviceState.RECOVERY -> Color(0xFFFF5722)
        else -> Color(0xFF9E9E9E)
    }

    val indicatorColor by animateColorAsState(
        targetValue = color,
        label = "indicatorColor"
    )

    Box(
        modifier = modifier
            .defaultMinSize(8.dp, 8.dp)
            .background(color = indicatorColor, shape = CircleShape)
    )
}
