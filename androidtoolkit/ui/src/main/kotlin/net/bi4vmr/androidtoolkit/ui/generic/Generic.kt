package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice

@Composable
fun GenericPage(globalStateManager: GlobalStateManager) {
    val executors: List<ADBExecutor> by globalStateManager.executorsFlow.collectAsStateWithLifecycle()
    val selectedExecutor: ADBExecutor by globalStateManager.selectedExecutorFlow.collectAsStateWithLifecycle()
    val devices: List<ADBDevice> by globalStateManager.devicesFlow.collectAsStateWithLifecycle()
    val selectedDevice: ADBDevice? by globalStateManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                text = "设备列表",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                executors.forEach { executor ->
                    Text(
                        executor.getDisplayName(),
                        modifier = Modifier.clickable {
                            globalStateManager.selectExecutor(executor)
                        })

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            DeviceList(
                globalStateManager = globalStateManager,
                devices = devices,
                selected = selectedDevice,
                onSelect = { globalStateManager.selectDevice(it) },
                modifier = Modifier.width(280.dp).fillMaxHeight()
            )
        }

        VerticalDivider()
        DevicePanel(
            device = selectedDevice,
            modifier = Modifier.weight(1F).fillMaxHeight()
        )
    }
}


@Composable
private fun DeviceList(
    globalStateManager: GlobalStateManager,
    devices: List<ADBDevice>,
    selected: ADBDevice?,
    onSelect: (ADBDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
            var host: String by remember { mutableStateOf("127.0.0.1") }
            var loading: Boolean by remember { mutableStateOf(false) }
            var errorInfo: String? by remember { mutableStateOf(null) }
            val scope = rememberCoroutineScope()

            Dialog(
                // 连接过程中不允许关闭对话框
                onDismissRequest = { if (!loading) connectProxyDialogState = false },
            ) {

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        Text("连接 ADB 转发器")

                        Text("主机名或 IP 地址：")
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            enabled = !loading
                        )

                        if (errorInfo != null) {
                            Text(errorInfo ?: "-", color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            enabled = !loading,
                            onClick = {
                                loading = true
                                errorInfo = null
                                scope.launch {
                                    try {
                                        globalStateManager.addRemoteExecutor(host)
                                        connectProxyDialogState = false
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        errorInfo = e.message ?: "连接失败，请检查主机名或 IP 地址是否正确"
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("连接中…")
                            } else {
                                Text("连接")
                            }
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
        // Text(device.getModel(), style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        InfoRow("序列号", device.getSN())
        InfoRow("状态", device.getState().name)
        // InfoRow("厂商", device.getManufacturer())
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
