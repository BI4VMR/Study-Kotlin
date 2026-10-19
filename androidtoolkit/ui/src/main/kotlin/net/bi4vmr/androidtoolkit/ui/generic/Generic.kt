package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.core.ADBExecutor
import net.bi4vmr.androidtoolkit.core.Device
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.constant.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice

@Composable
fun GenericPage(globalStateManager: GlobalStateManager) {
    val executors: List<ADBExecutor> by globalStateManager.executorsFlow.collectAsStateWithLifecycle()
    val selectedExecutor: ADBExecutor? by globalStateManager.selectedExecutorFlow.collectAsStateWithLifecycle()
    val devices: List<Device> by globalStateManager.devicesFlow.collectAsStateWithLifecycle()
    val selectedDevice: Device? by globalStateManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    LaunchedEffect(devices) {
        println(devices)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                text = "设备列表",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Row {
                executors.forEach { executor ->
                    Text("${executor.getDisplayName()}", modifier = Modifier.clickable {
                        globalStateManager.selectExecutor(executor)
                    })
                }
            }

            DeviceList(
                devices = devices,
                selected = selectedDevice,
                onSelect = { globalStateManager.selectDevice(it) },
                modifier = Modifier.width(280.dp).fillMaxHeight()
            )
        }

        VerticalDivider()
        // DevicePanel(
        //     device = selectedDevice,
        //     modifier = Modifier.weight(1F).fillMaxHeight()
        // )
    }
}


@Composable
private fun DeviceList(
    devices: List<Device>,
    selected: Device?,
    onSelect: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "设备列表 (${devices.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1F),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(devices, key = { it.getSN() }) { device ->
                // val isSelected = (device == sele
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(device) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (false) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(device.getSN(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            device.getSN(),
                            style = MaterialTheme.typography.labelSmall,
                            // color = if (device.getStateName() == DeviceState.ONLINE) MaterialTheme.colorScheme.primary
                            // else MaterialTheme.colorScheme.error
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
            Dialog(
                onDismissRequest = { connectDeviceDialogState = false },
            ) {
                var host: String by remember { mutableStateOf("") }
                var state: Boolean by remember { mutableStateOf(false) }
                var errorInfo: String? by remember { mutableStateOf(null) }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        Text("连接 Android 设备")

                        Text("主机名或 IP 地址：")
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it }
                        )

                        if (errorInfo != null) {
                            Text(errorInfo ?: "-")
                        }

                        Button(
                            onClick = {
                                val result = ADBController.connectRemoteDevice(host)
                                if (result) {
                                    connectDeviceDialogState = false
                                } else {
                                    errorInfo = "连接失败，请检查主机名或 IP 地址是否正确"
                                }
                            }
                        ) {
                            Text("连接")
                        }
                    }
                }
            }
        }

        if (connectProxyDialogState) {
            Dialog(
                onDismissRequest = { connectProxyDialogState = false },
            ) {
                var host: String by remember { mutableStateOf("") }
                var state: Boolean by remember { mutableStateOf(false) }
                var errorInfo: String? by remember { mutableStateOf(null) }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        Text("连接 ADB 转发器")

                        Text("主机名或 IP 地址：")
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it }
                        )

                        if (errorInfo != null) {
                            Text(errorInfo ?: "-")
                        }

                        Button(
                            onClick = {
                                val result = ADBController.connectRemoteDevice(host)
                                if (result) {
                                    connectDeviceDialogState = false
                                } else {
                                    errorInfo = "连接失败，请检查主机名或 IP 地址是否正确"
                                }
                            }
                        ) {
                            Text("连接")
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DevicePanel(device: ADBDevice?, modifier: Modifier = Modifier) {
    if (device == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("请在左侧选择一台设备", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val enabled = device.getState() == DeviceState.ONLINE
    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(device.getModel(), style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        InfoRow("序列号", device.getSN())
        InfoRow("状态", device.getState().name)
        InfoRow("厂商", device.getManufacturer())
        InfoRow("Android版本", "-")
        // InfoRow("SDK", device.sdk.toString())
        // InfoRow("CPU架构", device.abi)
        // InfoRow("Root", if (device.isRoot) "已获取" else "未获取")
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { /* TODO 重启 */ }, enabled = enabled) { Text("重启") }
            Button(onClick = { /* TODO 截图 */ }, enabled = enabled) { Text("截图") }
            OutlinedButton(onClick = { /* TODO Root */ }, enabled = enabled) { Text("Root") }
        }
    }
}


@Composable
private fun InfoRow(name: String, value: String) {
    Row {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
